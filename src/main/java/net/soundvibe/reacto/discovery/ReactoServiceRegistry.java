package net.soundvibe.reacto.discovery;

import io.vertx.core.logging.*;
import io.vertx.servicediscovery.*;
import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.mappers.ServiceRegistryMapper;
import net.soundvibe.reacto.server.ServiceRecords;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.soundvibe.reacto.discovery.DiscoverableServices.publishRecord;

/**
 * @author linas on 17.1.9.
 */
public final class ReactoServiceRegistry implements ServiceRegistry, ServiceDiscoveryLifecycle, CommandExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReactoServiceRegistry.class);

    private final AtomicBoolean isClosed = new AtomicBoolean(true);

    private final ServiceDiscovery serviceDiscovery;
    private final ServiceRegistryMapper mapper;

    public ReactoServiceRegistry(ServiceDiscovery serviceDiscovery, ServiceRegistryMapper mapper) {
        Objects.requireNonNull(serviceDiscovery, "serviceDiscovery cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");
        this.serviceDiscovery = serviceDiscovery;
        this.mapper = mapper;
    }

    @Override
    public Observable<Event> execute(final Command command) {
        return execute(command, LoadBalancers.ROUND_ROBIN);
    }

    public Observable<Event> execute(Command command, LoadBalancer<EventHandler> loadBalancer) {
        return DiscoverableServices.execute(command, serviceDiscovery, loadBalancer);
    }

    @Override
    public <E, C> Observable<E> execute(C command, Class<? extends E> eventClass, LoadBalancer<EventHandler> loadBalancer) {
        if (command instanceof Command && eventClass.equals(Event.class)) {
            //noinspection unchecked
            return (Observable<E>) execute((Command)command, loadBalancer);
        }
        return Observable.just(command)
                .map(mapper::toCommand)
                .flatMap(cmd -> execute(cmd, loadBalancer))
                .map(event -> mapper.toGenericEvent(event, eventClass));
    }

    @Override
    public Observable<Record> startDiscovery(Record record) {
        log.info("Starting service discovery...");
        return isClosed() ? publish(record)
                .subscribeOn(Factories.SINGLE_THREAD)
                .doOnCompleted(() -> isClosed.set(false)) :
                Observable.error(new IllegalStateException("Service discovery is already started"));
    }

    @Override
    public Observable<Record> closeDiscovery(Record record) {
        log.info("Closing service discovery...");
        return isOpen() ?
                Observable.just(record)
                        .subscribeOn(Factories.SINGLE_THREAD)
                        .observeOn(Factories.SINGLE_THREAD)
                        .flatMap(rec -> DiscoverableServices.removeIf(rec, ServiceRecords::areEquals, serviceDiscovery))
                        .doOnCompleted(() -> serviceDiscovery.release(serviceDiscovery.getReference(record)))
                        .doOnCompleted(serviceDiscovery::close)
                        .doOnCompleted(() -> isClosed.set(true)) :
                Observable.error(new IllegalStateException("Service discovery is already closed"));
    }

    @Override
    public Observable<Record> publish(Record record) {
        return publishRecord(record, serviceDiscovery);
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public boolean isOpen() {
        return !isClosed.get();
    }

    @Override
    public Observable<Record> cleanServices() {
        return DiscoverableServices.removeRecordsWithStatus(Status.DOWN, serviceDiscovery);
    }
}
