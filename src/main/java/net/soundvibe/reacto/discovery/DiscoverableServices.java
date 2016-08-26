package net.soundvibe.reacto.discovery;

import io.vertx.core.http.*;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.*;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.server.ServiceRecords;
import rx.Observable;

import java.time.Instant;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author OZY on 2016.08.26.
 */
public final class DiscoverableServices {

    private static final Logger log = LoggerFactory.getLogger(DiscoverableServices.class);

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
                    record.getMetadata().put(ServiceRecords.LAST_UPDATED, Instant.now());
                    publishRecord(doOnPublish, record, serviceDiscovery);
                } catch (Throwable e) {
                    log.error("Error while trying to publish the record on heartbeat: " + e);
                }
            }
        }, 0L, TimeUnit.MINUTES.toMillis(1L));
    }

     private static void publishRecord(Runnable doOnPublish, Record record, ServiceDiscovery serviceDiscovery) {
        removeExistingServiceRecordsIfPresent(serviceDiscovery, record);
        if (record.getRegistration() != null) {
            serviceDiscovery.update(record, recordEvent -> {
                doOnPublish.run();
                if (recordEvent.succeeded()) {
                    log.info("Service has been updated successfully: " + recordEvent.result().toJson());
                }
                if (recordEvent.failed()) {
                    log.error("Error when trying to updated the service: " + recordEvent.cause(), recordEvent.cause());
                }
            });
        } else {
            serviceDiscovery.publish(
                    record,
                    recordEvent -> {
                        doOnPublish.run();
                        if (recordEvent.succeeded()) {
                            log.info("Service has been published successfully: " + recordEvent.result().toJson());
                        }
                        if (recordEvent.failed()) {
                            log.error("Error when trying to publish the service: " + recordEvent.cause(), recordEvent.cause());
                        }
                    }
            );
        }
    }

    public static void closeDiscovery(ServiceDiscovery serviceDiscovery, Record record) {
        try {
            log.info("Closing service discovery...");
            DiscoverableServices.removeExistingServiceRecordsIfPresent(serviceDiscovery, record);
            serviceDiscovery.close();
        } catch (Throwable e) {
            log.warn("Error when closing service discovery: " + e);
        }
    }


    private static void removeExistingServiceRecordsIfPresent(ServiceDiscovery serviceDiscovery, Record newRecord) {
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
                                            serviceDiscovery.update(record.setStatus(Status.DOWN), e -> {
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
