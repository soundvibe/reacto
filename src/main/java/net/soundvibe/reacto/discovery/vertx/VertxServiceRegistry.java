package net.soundvibe.reacto.discovery.vertx;

import io.vertx.core.json.JsonArray;
import io.vertx.core.logging.*;
import io.vertx.servicediscovery.*;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.discovery.*;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.mappers.ServiceRegistryMapper;
import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.server.vertx.ServiceRecords;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.*;
import rx.Observable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import static net.soundvibe.reacto.discovery.vertx.DiscoverableServices.publishRecord;

/**
 * @author linas on 17.1.9.
 */
public final class VertxServiceRegistry implements ServiceRegistry, ServiceDiscoveryLifecycle, CommandExecutor {

    private static final Logger log = LoggerFactory.getLogger(VertxServiceRegistry.class);

    private final AtomicBoolean isClosed = new AtomicBoolean(true);
    private final AtomicReference<Record> record = new AtomicReference<>();
    private final ServiceDiscovery serviceDiscovery;
    private final ServiceRegistryMapper mapper;

    public VertxServiceRegistry(ServiceDiscovery serviceDiscovery, ServiceRegistryMapper mapper) {
        Objects.requireNonNull(serviceDiscovery, "serviceDiscovery cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        this.serviceDiscovery = serviceDiscovery;
        this.mapper = mapper;
    }

    @Override
    public Observable<Event> execute(final Command command) {
        return execute(command, LoadBalancers.ROUND_ROBIN);
    }

    private Observable<Event> execute(Command command, LoadBalancer<EventHandler> loadBalancer) {
        return DiscoverableServices.execute(command, serviceDiscovery, loadBalancer);
    }

    @Override
    public <E, C> Observable<? extends E> execute(C command, Class<? extends E> eventClass, LoadBalancer<EventHandler> loadBalancer) {
        if (command == null) return Observable.error(new IllegalArgumentException("command cannot be null"));
        if (eventClass == null) return Observable.error(new IllegalArgumentException("eventClass cannot be null"));
        if (loadBalancer == null) return Observable.error(new IllegalArgumentException("loadBalancer cannot be null"));

        if (command instanceof Command && eventClass.isAssignableFrom(Event.class)) {
            //noinspection unchecked
            return (Observable<E>) execute((Command)command, loadBalancer);
        }

        return Observable.just(command)
                .map(cmd -> mapper.toCommand(cmd, eventClass))
                .concatMap(typedCommand -> execute(typedCommand, loadBalancer)).onBackpressureBuffer()
                .map(event -> mapper.toGenericEvent(event, eventClass));
    }

    @Override
    public Observable<Any> startDiscovery(ServiceRecord serviceRecord, CommandRegistry commandRegistry) {
        log.info("Starting service discovery...");
        try {
            return isClosed() ?
                    Observable.just(serviceRecord)
                            .map(rec -> record.updateAndGet(__ -> createVertxRecord(rec, commandRegistry)))
                            .flatMap(this::publish)
                            .doOnNext(this::startHeartBeat)
                            .doOnNext(rec -> Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                                log.info("Executing shutdown hook...");
                                if (isOpen()) {
                                    closeDiscovery().subscribe(
                                            r -> log.debug("Service discovery closed successfully"),
                                            e -> log.debug("Error when closing service discovery: " + e)
                                    );
                                }
                            })))
                            .map(rec -> Any.VOID)
                            .subscribeOn(Factories.SINGLE_THREAD)
                            .doOnCompleted(() -> isClosed.set(false)) :
                    Observable.error(new IllegalStateException("Service discovery is already started"));
        } catch (Throwable e) {
            return Observable.error(e);
        }
    }

    @Override
    public Observable<Any> closeDiscovery() {
        log.info("Closing service discovery...");
        return isOpen() ?
                Observable.just(record.get())
                        .subscribeOn(Factories.SINGLE_THREAD)
                        .observeOn(Factories.SINGLE_THREAD)
                        .flatMap(rec -> DiscoverableServices.removeIf(rec, ServiceRecords::areEquals, serviceDiscovery))
                        .map(rec -> Any.VOID)
                        .doOnCompleted(() -> serviceDiscovery.release(serviceDiscovery.getReference(record.get())))
                        .doOnCompleted(serviceDiscovery::close)
                        .doOnCompleted(() -> isClosed.set(true)) :
                Observable.error(new IllegalStateException("Service discovery is already closed"));
    }


    public Observable<Record> publish(Record record) {
        return publishRecord(record, serviceDiscovery);
    }


    public boolean isClosed() {
        return isClosed.get();
    }


    public boolean isOpen() {
        return !isClosed.get();
    }

    public Observable<Record> cleanServices() {
        return DiscoverableServices.removeRecordsWithStatus(Status.DOWN, serviceDiscovery);
    }

    public static Record createVertxRecord(ServiceRecord serviceRecord, CommandRegistry commandRegistry) {
        final String host = serviceRecord.location.asString(ServiceRecord.LOCATION_HOST).orElseGet(WebUtils::getLocalAddress);
        final Integer port = serviceRecord.location.asInteger(ServiceRecord.LOCATION_PORT)
                .orElseThrow(() -> new IllegalArgumentException("port is not found in serviceRecord location"));
        return HttpEndpoint.createRecord(
               serviceRecord.name,
               host,
               port,
               serviceRecord.location.asString(ServiceRecord.LOCATION_ROOT).orElse("/"),
               new io.vertx.core.json.JsonObject()
                        .put(ServiceRecord.METADATA_VERSION, serviceRecord.metaData.asString(ServiceRecord.METADATA_VERSION)
                                .orElse("UNKNOWN"))
                        .put(ServiceRecord.METADATA_COMMANDS, commandsToJsonArray(commandRegistry))
       );
    }

    static JsonArray commandsToJsonArray(CommandRegistry commands) {
        return commands.stream()
                .map(Pair::getKey)
                .map(commandDescriptor -> new io.vertx.core.json.JsonObject()
                        .put(CommandDescriptor.COMMAND, commandDescriptor.commandType)
                        .put(CommandDescriptor.EVENT, commandDescriptor.eventType)
                )
                .reduce(new JsonArray(), JsonArray::add, JsonArray::addAll);
    }

    private void startHeartBeat(Record record) {
        Scheduler.scheduleAtFixedInterval(TimeUnit.MINUTES.toMillis(1L), () -> {
            if (isOpen()) {
                publish(record)
                        .subscribe(rec -> log.debug("Heartbeat published record: " + rec),
                                throwable -> log.error("Error while trying to publish the record on heartbeat: " + throwable),
                                () -> log.debug("Heartbeat completed successfully"));
            } else {
                log.info("Skipping heartbeat because service discovery is closed");
            }
        }, "service-discovery-heartbeat");
    }

}
