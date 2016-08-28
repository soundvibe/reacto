package net.soundvibe.reacto.discovery;

import io.vertx.core.http.*;
import io.vertx.servicediscovery.*;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.server.ServiceRecords;
import rx.Observable;

import java.time.Instant;
import java.util.List;

import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author OZY on 2016.08.26.
 */
public final class DiscoverableServices {

    public static Observable<WebSocketStream> find(String serviceName, ServiceDiscovery serviceDiscovery, LoadBalancer loadBalancer) {
        return Observable.<HttpClient>create(subscriber ->
            serviceDiscovery.getRecords(record ->
                    serviceName.equals(record.getName()) && ServiceRecords.isUpdatedRecently(record),
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

}
