package net.soundvibe.reacto.client.events.vertx;

import io.vertx.core.http.*;
import io.vertx.core.logging.*;
import io.vertx.servicediscovery.*;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.discovery.vertx.DiscoverableServices;
import net.soundvibe.reacto.server.vertx.ServiceRecords;
import net.soundvibe.reacto.types.*;
import rx.Observable;
import rx.internal.util.ActionObserver;

import java.util.Objects;
import java.util.function.BiFunction;

import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxDiscoverableEventHandler implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(VertxDiscoverableEventHandler.class);

    private final Record record;
    private final BiFunction<WebSocketStream, Command, Observable<Event>> eventHandler;
    private final ServiceDiscovery serviceDiscovery;

    public VertxDiscoverableEventHandler(Record record, ServiceDiscovery serviceDiscovery,
                                         BiFunction<WebSocketStream, Command, Observable<Event>> eventHandler) {
        Objects.requireNonNull(record, "record cannot be null");
        Objects.requireNonNull(serviceDiscovery, "serviceDiscovery cannot be null");
        Objects.requireNonNull(eventHandler, "eventHandler cannot be null");
        this.record = record;
        this.serviceDiscovery = serviceDiscovery;
        this.eventHandler = eventHandler;
    }

    @Override
    public Observable<Event> toObservable(Command command) {
        return Observable.just(record)
                .<HttpClient>map(rec -> serviceDiscovery.getReference(rec).get())
                .map(httpClient -> httpClient.websocketStream(includeStartDelimiter(includeEndDelimiter(record.getName()))))
                .concatMap(webSocketStream -> eventHandler.apply(webSocketStream, command)
                        .onBackpressureBuffer()
                        .onErrorResumeNext(this::handleError))
                ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VertxDiscoverableEventHandler that = (VertxDiscoverableEventHandler) o;
        return Objects.equals(record, that.record);
    }

    @Override
    public int hashCode() {
        return Objects.hash(record);
    }

    private Observable<Event> handleError(Throwable error) {
        return DiscoverableServices.removeIf(record, ServiceRecords::areEquals, serviceDiscovery)
                .doOnEach(new ActionObserver<>(
                        r -> log.info("Unpublished record because of IO errors: " + r.toJson()),
                        throwable -> log.error("Error when trying to unpublish record because of IO errors: " + throwable),
                        () -> log.info("Unpublished record successfully"))
                )
                .onErrorResumeNext(throwable -> Observable.error(error))
                .flatMap(r -> Observable.error(error))
                ;
    }

    @Override
    public String name() {
        return record.getName();
    }
}
