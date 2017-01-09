package net.soundvibe.reacto.discovery;

import io.vertx.core.logging.*;
import io.vertx.servicediscovery.*;
import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.server.ServiceRecords;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;

import java.util.concurrent.atomic.AtomicBoolean;

import static net.soundvibe.reacto.discovery.DiscoverableServices.publishRecord;

/**
 * @author linas on 17.1.9.
 */
public final class ReactoServiceRegistry implements ServiceRegistry, ServiceDiscoveryLifecycle {

    private static final Logger log = LoggerFactory.getLogger(ReactoServiceRegistry.class);

    private final AtomicBoolean isClosed = new AtomicBoolean(true);

    private final ServiceDiscovery serviceDiscovery;

    public ReactoServiceRegistry(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @Override
    public Observable<CommandExecutor> find(String commandName, LoadBalancer<EventHandler> loadBalancer) {
        return DiscoverableServices.findCommand(commandName, serviceDiscovery, loadBalancer);
    }

    @Override
    public Observable<Event> execute(Command command, LoadBalancer<EventHandler> loadBalancer) {
        return DiscoverableServices.execute(command, serviceDiscovery, loadBalancer);
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
