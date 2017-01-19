package net.soundvibe.reacto.client.events.vertx;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import io.vertx.core.logging.*;
import io.vertx.servicediscovery.*;
import net.soundvibe.reacto.errors.*;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.discovery.vertx.VertxServiceRegistry;
import net.soundvibe.reacto.internal.InternalEvent;
import net.soundvibe.reacto.mappers.Mappers;
import net.soundvibe.reacto.server.vertx.handlers.WebSocketFrameHandler;
import net.soundvibe.reacto.types.*;
import rx.*;

import java.util.Objects;
import java.util.function.*;

import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxDiscoverableEventHandler implements EventHandler, Function<ServiceRecord, EventHandler> {

    private static final Logger log = LoggerFactory.getLogger(VertxDiscoverableEventHandler.class);

    private final ServiceRecord serviceRecord;
    private final Record record;
    private final ServiceDiscovery serviceDiscovery;

    public VertxDiscoverableEventHandler(ServiceRecord serviceRecord, ServiceDiscovery serviceDiscovery) {
        Objects.requireNonNull(serviceRecord, "serviceRecord cannot be null");
        Objects.requireNonNull(serviceDiscovery, "serviceDiscovery cannot be null");
        this.serviceRecord = serviceRecord;
        this.record = VertxServiceRegistry.createVertxRecord(serviceRecord);
        this.serviceDiscovery = serviceDiscovery;
    }

    @Override
    public Observable<Event> observe(Command command) {
        return Observable.just(record)
                .<HttpClient>map(rec -> serviceDiscovery.getReference(rec).get())
                .map(httpClient -> httpClient.websocketStream(includeStartDelimiter(includeEndDelimiter(record.getName()))))
                .concatMap(webSocketStream -> observe(webSocketStream, command)
                        .onBackpressureBuffer());
    }

    @Override
    public ServiceRecord serviceRecord() {
        return serviceRecord;
    }

    public static EventHandler create(ServiceRecord serviceRecord, ServiceDiscovery serviceDiscovery) {
        return new VertxDiscoverableEventHandler(serviceRecord, serviceDiscovery);
    }

    @Override
    public EventHandler apply(ServiceRecord serviceRecord) {
        return create(serviceRecord, serviceDiscovery);
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

    private static void checkForEvents(WebSocket webSocket, Subscriber<? super Event> subscriber) {
        webSocket
                .frameHandler(new WebSocketFrameHandler(buffer -> {
                    try {
                        if (!subscriber.isUnsubscribed()) {
                            handleEvent(Mappers.fromBytesToInternalEvent(buffer.getBytes()), subscriber);
                        }
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private static void handleEvent(InternalEvent internalEvent, Subscriber<? super Event> subscriber) {
        log.debug("InternalEvent has been received and is being handled: " + internalEvent);
        switch (internalEvent.eventType) {
            case NEXT: {
                subscriber.onNext(Mappers.fromInternalEvent(internalEvent));
                break;
            }
            case ERROR: {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onError(internalEvent.error
                            .orElse(ReactiveException.from(new UnknownError("Unknown error from internalEvent: " + internalEvent))));
                }
                break;
            }
            case COMPLETED: {
                if (!subscriber.isUnsubscribed()) {
                    subscriber.onCompleted();
                }
                break;
            }
        }
    }

    public static Observable<Event> observe(WebSocketStream webSocketStream, Command command) {
        return Observable.create(subscriber -> {
            try {
                webSocketStream
                        .exceptionHandler(subscriber::onError)
                        .handler(webSocket -> {
                            try {
                                webSocket.setWriteQueueMaxSize(Integer.MAX_VALUE).closeHandler(__ -> {
                                    if (!subscriber.isUnsubscribed()) {
                                        subscriber.onError(new ConnectionClosedUnexpectedly(
                                                "WebSocket connection closed without completion for command: " + command));
                                    }
                                }).exceptionHandler(subscriber::onError);
                                checkForEvents(webSocket, subscriber);
                                sendCommandToExecutor(command, webSocket);
                            } catch (Throwable e) {
                                subscriber.onError(e);
                            }
                        });
            } catch (Throwable e) {
                subscriber.onError(e);
            }
        });
    }

    private static void sendCommandToExecutor(Command command, WebSocket webSocket) {
        log.debug("Sending command to executor: " + command);
        final byte[] bytes = Mappers.commandToBytes(command);
        webSocket.writeBinaryMessage(Buffer.buffer(bytes));
    }

    @Override
    public String name() {
        return record.getName();
    }


}
