package net.soundvibe.reacto.discovery;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.Status;
import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.commands.CommandExecutors;
import net.soundvibe.reacto.client.commands.Services;
import net.soundvibe.reacto.server.ServiceRecords;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;

import java.time.Instant;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * @author OZY on 2016.08.28.
 */
@SuppressWarnings("ThrowableResultOfMethodCallIgnored")
public final class DiscoverableService {

    private static final Logger log = LoggerFactory.getLogger(DiscoverableService.class);

    private final AtomicBoolean isClosed = new AtomicBoolean(true);

    public final ServiceDiscovery serviceDiscovery;

    public DiscoverableService(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    public Observable<CommandExecutor> find(String serviceName) {
        return CommandExecutors.find(Services.ofMain(serviceName, serviceDiscovery));
    }

    public Observable<CommandExecutor> find(String serviceName, Predicate<Record> filter) {
        return CommandExecutors.find(Services.ofMain(serviceName, serviceDiscovery), filter);
    }


    public Observable<CommandExecutor> find(String serviceName, LoadBalancer loadBalancer) {
        return CommandExecutors.find(Services.ofMain(serviceName, serviceDiscovery), loadBalancer);
    }

    public Observable<CommandExecutor> find(String serviceName, LoadBalancer loadBalancer, Predicate<Record> filter) {
        return CommandExecutors.find(Services.ofMain(serviceName, serviceDiscovery), loadBalancer, filter);
    }

    public Observable<CommandExecutor> find(String mainServiceName, String fallbackServiceName) {
        return CommandExecutors.find(Services.ofMainAndFallback(mainServiceName, fallbackServiceName, serviceDiscovery));
    }

    public Observable<CommandExecutor> find(String mainServiceName, String fallbackServiceName, Predicate<Record> filter) {
        return CommandExecutors.find(Services.ofMainAndFallback(mainServiceName, fallbackServiceName, serviceDiscovery), filter);
    }

    public Observable<CommandExecutor> find(String mainServiceName, String fallbackServiceName, LoadBalancer loadBalancer) {
        return CommandExecutors.find(Services.ofMainAndFallback(mainServiceName, fallbackServiceName, serviceDiscovery), loadBalancer);
    }

    public Observable<CommandExecutor> find(String mainServiceName, String fallbackServiceName, LoadBalancer loadBalancer, Predicate<Record> filter) {
        return CommandExecutors.find(Services.ofMainAndFallback(mainServiceName, fallbackServiceName, serviceDiscovery), loadBalancer, filter);
    }

    public void startHeartBeat(Runnable doOnPublish, Record record) {
        new Timer("service-discovery-heartbeat", true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (isOpen()) {
                        publishRecord(record)
                                .doOnTerminate(doOnPublish::run)
                                .subscribe(rec -> log.info("Heartbeat published record: " + rec),
                                        throwable -> log.error("Error while trying to publish the record on heartbeat: " + throwable),
                                        () -> log.info("Heartbeat completed successfully"));
                    } else {
                        log.info("Skipping heartbeat because service discovery is closed");
                    }
                } catch (Throwable e) {
                    log.error("Error while trying to publish the record on heartbeat: " + e);
                }
            }
        }, TimeUnit.MINUTES.toMillis(1L), TimeUnit.MINUTES.toMillis(1L));
    }

    private Observable<Record> publishRecord(Record record) {
        return Observable.just(record)
                .flatMap(rec -> updateServiceRecordsStatus(rec, Status.DOWN, ServiceRecords::isDown))
                .map(rec -> {
                    rec.getMetadata().put(ServiceRecords.LAST_UPDATED, Instant.now());
                    return rec.setStatus(Status.UP);
                })
                .flatMap(rec -> Observable.create(subscriber -> {
                    if (rec.getRegistration() != null) {
                        serviceDiscovery.update(record, recordEvent -> {
                            if (recordEvent.succeeded()) {
                                log.info("Service has been updated successfully: " + recordEvent.result().toJson());
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onNext(recordEvent.result());
                                    subscriber.onCompleted();
                                }
                            }
                            if (recordEvent.failed()) {
                                log.error("Error when trying to updated the service: " + recordEvent.cause(), recordEvent.cause());
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onError(recordEvent.cause());
                                }
                            }
                        });
                    } else {
                        serviceDiscovery.publish(rec, recordEvent -> {
                            if (recordEvent.succeeded()) {
                                log.info("Service has been published successfully: " + recordEvent.result().toJson());
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onNext(recordEvent.result());
                                    subscriber.onCompleted();
                                }
                            }
                            if (recordEvent.failed()) {
                                log.error("Error when trying to publish the service: " + recordEvent.cause(), recordEvent.cause());
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onError(recordEvent.cause());
                                }
                            }
                        });
                    }
                }));
    }

    public Observable<Record> startDiscovery(Record record) {
        log.info("Starting service discovery...");
        return isClosed() ? publishRecord(record)
                .subscribeOn(Factories.SINGLE_THREAD)
                .doOnCompleted(() -> isClosed.set(false)) :
                Observable.error(new IllegalStateException("Service discovery is already started"));
    }

    public Observable<Record> closeDiscovery(Record record) {
        log.info("Closing service discovery...");
        return isOpen() ?
                Observable.just(record)
                        .subscribeOn(Factories.SINGLE_THREAD)
                        .observeOn(Factories.SINGLE_THREAD)
                .flatMap(rec -> updateServiceRecordsStatus(rec, Status.DOWN, ServiceRecords::AreEquals))
                .doOnCompleted(() -> serviceDiscovery.release(serviceDiscovery.getReference(record)))
                .doOnCompleted(serviceDiscovery::close)
                .doOnCompleted(() -> isClosed.set(true)) :
                Observable.error(new IllegalStateException("Service discovery is already closed"));
    }

    private Observable<Record> updateServiceRecordsStatus(Record newRecord, Status status,
                                                   BiFunction<Record, Record, Boolean> filter) {
        return Observable.create(subscriber ->
            serviceDiscovery.getRecords(
                    record -> filter.apply(record, newRecord),
                    false,
                    event -> {
                        if (event.succeeded()) {
                            if (event.result().isEmpty() && !subscriber.isUnsubscribed()) {
                                subscriber.onNext(newRecord);
                                subscriber.onCompleted();
                                return;
                            }
                            event.result().stream()
                                    .peek(record -> serviceDiscovery.release(serviceDiscovery.getReference(record)))
                                    .forEach(record ->
                                        serviceDiscovery.update(record.setStatus(status), e -> {
                                            if (e.failed() && (!subscriber.isUnsubscribed())) {
                                                subscriber.onError(e.cause());
                                                return;
                                            }
                                            if (e.succeeded() && (!subscriber.isUnsubscribed())) {
                                                subscriber.onNext(newRecord);
                                                subscriber.onCompleted();
                                            }
                                    }));
                        }
                        if (event.failed()) {
                            log.info("No matching records: " + event.cause());
                            subscriber.onError(event.cause());
                        }
                    }));
    }

    public boolean isClosed() {
        return isClosed.get();
    }

    public boolean isOpen() {
        return !isClosed.get();
    }


}
