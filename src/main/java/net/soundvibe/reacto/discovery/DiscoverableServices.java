package net.soundvibe.reacto.discovery;

import io.vertx.core.http.*;
import io.vertx.servicediscovery.*;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.server.ServiceRecords;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;

import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

import static java.util.Comparator.comparing;
import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author OZY on 2016.08.26.
 */
public final class DiscoverableServices {

    /**
     * Finds running service using service discovery and client load balancer
     * @param serviceName The name of the service to look for
     * @param serviceDiscovery service discovery to use when looking for a service
     * @param loadBalancer load balancer to use when multiple services are found
     * @return WebSocketStream observable, which emits single WebSocketStream if service is found successfully
     */
    public static Observable<WebSocketStream> find(String serviceName,
                                                   ServiceDiscovery serviceDiscovery,
                                                   LoadBalancer loadBalancer) {
        return find(serviceName, Factories.ALL_RECORDS, serviceDiscovery, loadBalancer);
    }

    /**
     * Finds running service using service discovery and client load balancer
     * @param serviceName The name of the service to look for
     * @param filter additional predicate to filter found services
     * @param serviceDiscovery service discovery to use when looking for a service
     * @param loadBalancer load balancer to use when multiple services are found
     * @return WebSocketStream observable, which emits single WebSocketStream if service is found successfully
     */
    public static Observable<WebSocketStream> find(String serviceName,
                                                   Predicate<Record> filter,
                                                   ServiceDiscovery serviceDiscovery,
                                                   LoadBalancer loadBalancer) {
        return Observable.<HttpClient>create(subscriber ->
            serviceDiscovery.getRecords(record ->
                    serviceName.equals(record.getName()) && ServiceRecords.isUpdatedRecently(record) && filter.test(record),
                    false,
                    asyncClients -> {
                        if (asyncClients.succeeded() && !subscriber.isUnsubscribed()) {
                            final List<Record> records = asyncClients.result();
                            if (!records.isEmpty()) {
                                final Instant now = Instant.now();
                                records.sort(comparing(rec -> rec.getMetadata().getInstant(ServiceRecords.LAST_UPDATED, now)));
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

}
