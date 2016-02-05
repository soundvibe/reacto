package reactive.fp.client.events;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketStream;
import reactive.fp.mappers.Mappers;
import reactive.fp.server.handlers.WebSocketFrameHandler;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import reactive.fp.types.ReactiveException;
import reactive.fp.utils.Factories;
import rx.Observable;
import rx.Subscriber;

import java.net.URI;
import java.util.Objects;

import static reactive.fp.mappers.Mappers.fromBytesToEvent;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxWebSocketEventHandler implements EventHandler {

    private final URI wsUrl;
    private final Vertx vertx;

    public VertxWebSocketEventHandler(URI wsUrl) {
        Objects.requireNonNull(wsUrl, "WebSocket URI cannot be null");
        this.wsUrl = wsUrl;
        this.vertx = Factories.vertx();
    }

    @Override
    public Observable<Event> toObservable(Command command) {
        return Observable.using(() -> vertx.createHttpClient(new HttpClientOptions()),
                httpClient -> webSocketStreamObservable(httpClient, command),
                HttpClient::close);
    }

    private Observable<Event> webSocketStreamObservable(HttpClient httpClient, Command command) {
        try {
            final WebSocketStream webSocketStream = httpClient.websocketStream(getPortFromURI(wsUrl), wsUrl.getHost(), wsUrl.getPath());
            return observe(webSocketStream, command);
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
                                webSocket.setWriteQueueMaxSize(Integer.MAX_VALUE);
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
                            handleEvent(fromBytesToEvent(buffer.getBytes()), subscriber);
                        }
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private void handleEvent(Event event, Subscriber<? super Event> subscriber) {
        switch (event.eventType) {
            case NEXT: {
                subscriber.onNext(event);
/*                if (event.payload == null) {
                    subscriber.onError(new NullPointerException("Payload was null for event: " + event));
                } else if (aClass.isAssignableFrom(event.payload.getClass())) {
                    subscriber.onNext(event);
                } else {
                    subscriber.onError(new ClassCastException("Invalid event payload type. Received " +
                            event.payload.getClass() + " but expected " + aClass));
                }*/
                break;
            }
            case ERROR: {
                subscriber.onError(event.error.orElse(ReactiveException.from(new UnknownError("Unknown error from event: " + event))));
                break;
            }
            case COMPLETED: {
                subscriber.onCompleted();
                break;
            }
        }
    }

    private int getPortFromURI(URI uri) {
        return uri.getPort() == -1 ?
                80:
                uri.getPort();
    }

    private void sendCommandToExecutor(Command command, WebSocket webSocket) {
        final byte[] bytes = Mappers.commandToBytes(command);
        webSocket.writeBinaryMessage(Buffer.buffer(bytes));
    }
}
