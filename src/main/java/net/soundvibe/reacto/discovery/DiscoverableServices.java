package net.soundvibe.reacto.discovery;

import io.vertx.core.http.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.*;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.server.ServiceRecords;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;

import java.time.Instant;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author OZY on 2016.08.26.
 */
public final class DiscoverableServices {

    private static final Logger log = LoggerFactory.getLogger(DiscoverableServices.class);

    private static final AtomicBoolean isClosed = new AtomicBoolean(false);

    public static Observable<WebSocketStream> find(String serviceName, ServiceDiscovery serviceDiscovery, LoadBalancer loadBalancer) {
        return Observable.<HttpClient>create(subscriber ->
            serviceDiscovery.getRecords(record -> serviceName.equals(record.getName()),
                    false,
                    asyncClients -> {
                        if (asyncClients.succeeded() && !subscriber.isUnsubscribed()) {
                            final List<Record> records = asyncClients.result();
                            if (!records.isEmpty()) {
                                records.sort((rec1, rec2) -> rec1.getMetadata().getInstant(ServiceRecords.LAST_UPDATED, Instant.now())
                                        .compareTo(rec2.getMetadata().getInstant(ServiceRecords.LAST_UPDATED, Instant.now())));
                                final Record record = loadBalancer.balance(records);
                                subscriber.onNext(serviceDiscovery.getReference(record).get());
                            }
                            subscriber.onCompleted();
                        }
                        if (asyncClients.failed() && !subscriber.isUnsubscribed()) {
                            subscriber.onError(new CannotDiscoverService("Unable to find service: " + serviceName, asyncClients.cause()));
                        }
                    })
        ).map(httpClient -> httpClient.websocketStream(includeStartDelimiter(includeEndDelimiter(serviceName))));
    }

    public static void startHeartBeat(Runnable doOnPublish, Record record, ServiceDiscovery serviceDiscovery) {
        new Timer("service-discovery-heartbeat", true).scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    if (!isClosed.get()) {
                        publishRecord(record, serviceDiscovery)
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
        }, 0L, TimeUnit.MINUTES.toMillis(1L));
    }


    private static Observable<Record> publishRecord(Record record, ServiceDiscovery serviceDiscovery) {
        return Observable.just(record)
                .subscribeOn(Factories.COMPUTATION)
                .doOnNext(rec -> updateServiceRecordsStatus(serviceDiscovery, rec, Status.DOWN))
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

    public static Observable<Record> startDiscovery(ServiceDiscovery serviceDiscovery, Record record) {
        log.info("Starting service discovery...");
        return publishRecord(record, serviceDiscovery)
                .doOnCompleted(() -> isClosed.set(false));
    }

    public static void closeDiscovery(ServiceDiscovery serviceDiscovery, Record record) {
        try {
            log.info("Closing service discovery...");
            DiscoverableServices.updateServiceRecordsStatus(serviceDiscovery, record, Status.DOWN);
            serviceDiscovery.close();
            isClosed.set(true);
        } catch (Throwable e) {
            log.warn("Error when closing service discovery: " + e);
        }
    }

    private static void updateServiceRecordsStatus(ServiceDiscovery serviceDiscovery, Record newRecord, Status status) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            serviceDiscovery.getRecords(
                    existingRecord -> ServiceRecords.isDown(existingRecord, newRecord),
                    false,
                    event -> {
                        if (event.succeeded()) {
                            Observable.from(event.result())
                                    .doOnNext(record -> serviceDiscovery.release(serviceDiscovery.getReference(record)))
                                    .flatMap(record -> Observable.create(subscriber ->
                                            serviceDiscovery.update(record.setStatus(status), e -> {
                                                if (e.failed() && (!subscriber.isUnsubscribed())) {
                                                    subscriber.onError(e.cause());
                                                    return;
                                                }
                                                if (e.succeeded() && (!subscriber.isUnsubscribed())) {
                                                    subscriber.onNext(record);
                                                    subscriber.onCompleted();
                                                }
                                            })))
                                    .subscribe(record -> log.info("Record status set to DOWN: " + record),
                                            throwable -> {
                                                log.error("Error when setting record status: " + throwable);
                                                countDownLatch.countDown();
                                            },
                                            countDownLatch::countDown);
                        }
                        if (event.failed()) {
                            log.info("No matching records: " + event.cause());
                            countDownLatch.countDown();
                        }
                    });
            countDownLatch.await(1L, TimeUnit.MINUTES);
        } catch (Throwable e) {
            log.warn("Error when removing duplicates on service discovery: " + e);
        }
    }

}
