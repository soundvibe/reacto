package net.soundvibe.reacto.client.events;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.soundvibe.reacto.internal.InternalEvent;
import net.soundvibe.reacto.server.handlers.WebSocketFrameHandler;
import net.soundvibe.reacto.types.*;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.*;
import net.soundvibe.reacto.client.errors.ConnectionClosedUnexpectedly;
import net.soundvibe.reacto.mappers.Mappers;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;
import rx.Subscriber;

import java.net.URI;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxWebSocketEventHandler implements EventHandler {

    private static final Logger log = LoggerFactory.getLogger(VertxWebSocketEventHandler.class);

    private final Supplier<Pair<HttpClient, WebSocketStream>> supplier;

    public VertxWebSocketEventHandler(URI wsUrl) {
        Objects.requireNonNull(wsUrl, "WebSocket URI cannot be null");
        this.supplier = () -> {
            final HttpClient httpClient = Factories.vertx().createHttpClient(new HttpClientOptions());
            final WebSocketStream webSocketStream = httpClient.websocketStream(getPortFromURI(wsUrl), wsUrl.getHost(), wsUrl.getPath());
            return Pair.of(httpClient, webSocketStream);
        };
    }

    public VertxWebSocketEventHandler(Supplier<Pair<HttpClient, WebSocketStream>> supplier) {
        Objects.requireNonNull(supplier, "supplier cannot be null");
        this.supplier = supplier;
    }

    @Override
    public Observable<Event> toObservable(Command command) {
        try {
            final Pair<HttpClient, WebSocketStream> pair = supplier.get();
            return Observable.using(() -> pair.key,
                    httpClient -> observe(pair.value, command),
                    HttpClient::close);
        } catch (Throwable e) {
            return Observable.error(e);
        }
    }

    private Observable<Event> observe(WebSocketStream webSocketStream, Command command) {
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
                                sendCommandToExecutor(command, webSocket);
                                checkForEvents(webSocket, subscriber);
                            } catch (Throwable e) {
                                subscriber.onError(e);
                            }
                        });
            } catch (Throwable e) {
                subscriber.onError(e);
            }
        });
    }

    private void checkForEvents(WebSocket webSocket, Subscriber<? super Event> subscriber) {
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
    private void handleEvent(InternalEvent internalEvent, Subscriber<? super Event> subscriber) {
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

    private int getPortFromURI(URI uri) {
        return uri.getPort() == -1 ?
                80 :
                uri.getPort();
    }

    private void sendCommandToExecutor(Command command, WebSocket webSocket) {
        log.debug("Sending command to executor: " + command);
        final byte[] bytes = Mappers.commandToBytes(command);
        webSocket.writeBinaryMessage(Buffer.buffer(bytes));
    }
}
