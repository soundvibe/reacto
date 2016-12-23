package net.soundvibe.reacto.discovery;

import io.vertx.core.http.*;
import io.vertx.core.logging.*;
import io.vertx.servicediscovery.*;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.server.ServiceRecords;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;

import java.time.Instant;
import java.util.*;
import java.util.function.*;

import static java.util.Comparator.comparing;
import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author OZY on 2016.08.26.
 */
public final class DiscoverableServices {

    private static final Logger log = LoggerFactory.getLogger(DiscoverableServices.class);

    /**
     * Finds all available services using service discovery
     * @param serviceName The name of the service to look for
     * @param serviceDiscovery service discovery to use when looking for a service
     * @return WebSocketStream observable, which emits single WebSocketStream if service is found successfully
     */
    public static Observable<Record> find(String serviceName, ServiceDiscovery serviceDiscovery) {
        return find(serviceName, Factories.ALL_RECORDS, serviceDiscovery);
    }

    /**
     * Finds running service using service discovery and client load balancer
     * @param serviceName The name of the service to look for
     * @param filter additional predicate to filter found services
     * @param serviceDiscovery service discovery to use when looking for a service
     * @param loadBalancer load balancer to use when multiple services are found
     * @return WebSocketStream observable, which emits single WebSocketStream if service is found successfully
     */
    public static Observable<Record> find(String serviceName,
                                                   Predicate<Record> filter,
                                                   ServiceDiscovery serviceDiscovery) {
        return Observable.create(subscriber ->
            serviceDiscovery.getRecords(record ->
                    serviceName.equals(record.getName()) && ServiceRecords.isUpdatedRecently(record) && filter.test(record),
                    false,
                    asyncClients -> {
                        if (asyncClients.succeeded() && !subscriber.isUnsubscribed()) {
                            final List<Record> records = asyncClients.result();
                            if (!records.isEmpty()) {
                                final Instant now = Instant.now();
                                records.sort(comparing(rec -> rec.getMetadata().getInstant(ServiceRecords.LAST_UPDATED, now)));
                                records.forEach(subscriber::onNext);
                            }
                            subscriber.onCompleted();
                        }
                        if (asyncClients.failed() && !subscriber.isUnsubscribed()) {
                            subscriber.onError(new CannotDiscoverService("Unable to find service: " + serviceName, asyncClients.cause()));
                        }
                    })
        )//.map(httpClient -> httpClient.websocketStream(includeStartDelimiter(includeEndDelimiter(serviceName))))
                ;
    }

    public static Observable<Record> removeIf(Record newRecord,
                                              BiPredicate<Record, Record> filter,
                                              ServiceDiscovery serviceDiscovery) {
        return Observable.create(subscriber ->
                serviceDiscovery.getRecords(
                        existingRecord -> filter.test(existingRecord, newRecord),
                        true,
                        event -> {
                            if (event.succeeded()) {
                                if (event.result().isEmpty() && !subscriber.isUnsubscribed()) {
                                    subscriber.onNext(newRecord);
                                    subscriber.onCompleted();
                                    return;
                                }

                                Observable.from(event.result())
                                        .doOnNext(record -> serviceDiscovery.release(serviceDiscovery.getReference(record)))
                                        .flatMap(record -> Observable.<Record>create(s -> {
                                            serviceDiscovery.unpublish(record.getRegistration(), deleteEvent -> {
                                                if (deleteEvent.failed() && (!s.isUnsubscribed())) {
                                                    s.onError(deleteEvent.cause());
                                                }
                                                if (deleteEvent.succeeded() && (!s.isUnsubscribed())) {
                                                    s.onNext(record);
                                                    s.onCompleted();
                                                }
                                            });
                                        }))
                                        .subscribe(record -> log.info("Record was unpublished: " + record),
                                                throwable -> {
                                                    log.error("Error while trying to unpublish the record: " + throwable);
                                                    subscriber.onNext(newRecord);
                                                    subscriber.onCompleted();
                                                },
                                                () -> {
                                                    subscriber.onNext(newRecord);
                                                    subscriber.onCompleted();
                                                });
                            }
                            if (event.failed()) {
                                log.info("No matching records: " + event.cause());
                                subscriber.onError(event.cause());
                            }
                        }));
    }

}
