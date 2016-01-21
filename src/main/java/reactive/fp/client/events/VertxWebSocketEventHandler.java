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
import reactive.fp.utils.Factories;
import rx.Observable;
import rx.Subscriber;

import java.net.URI;
import java.util.Objects;

import static reactive.fp.mappers.Mappers.fromJsonToEvent;
import static reactive.fp.mappers.Mappers.messageToJsonBytes;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxWebSocketEventHandler<T> implements EventHandler<T> {

    private final URI wsUrl;
    private final Vertx vertx;
    private final Class<?> aClass;

    public VertxWebSocketEventHandler(URI wsUrl, Class<?> eventClass) {
        Objects.requireNonNull(wsUrl, "WebSocket URI cannot be null");
        Objects.requireNonNull(eventClass, "Event class cannot be null");
        this.wsUrl = wsUrl;
        this.aClass = eventClass;
        this.vertx = Factories.vertx();
    }

    @Override
    public Observable<Event<T>> toObservable(String commandName, Object arg) {
        return Observable.using(() -> vertx.createHttpClient(new HttpClientOptions()),
                httpClient -> webSocketStreamObservable(httpClient, commandName, arg),
                HttpClient::close);
    }

    private Observable<Event<T>> webSocketStreamObservable(HttpClient httpClient, String commandName, Object arg) {
        try {
            final WebSocketStream webSocketStream = httpClient.websocketStream(getPortFromURI(wsUrl), wsUrl.getHost(), wsUrl.getPath());
            return observe(webSocketStream, commandName, arg);
        } catch (Throwable e) {
            return Observable.error(e);
        }
    }

    private Observable<Event<T>> observe(WebSocketStream webSocketStream, String commandName, Object arg) {
        return Observable.create(subscriber -> {
            try {
                webSocketStream
                        .exceptionHandler(subscriber::onError)
                        .handler(webSocket -> {
                            try {
                                webSocket.setWriteQueueMaxSize(Integer.MAX_VALUE);
                                sendCommandToExecutor(commandName, arg, webSocket);
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

    private void checkForEvents(WebSocket webSocket, Subscriber<? super Event<T>> subscriber) {
        webSocket
                .frameHandler(new WebSocketFrameHandler(buffer -> {
                    try {
                        if (!subscriber.isUnsubscribed()) {
                            handleEvent(fromJsonToEvent(buffer.getBytes()), subscriber);
                        }
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }));
    }

    @SuppressWarnings("unchecked")
    private void handleEvent(Event<?> event, Subscriber<? super Event<T>> subscriber) {
        switch (event.eventType) {
            case NEXT: {
                if (event.payload == null) {
                    subscriber.onError(new NullPointerException("Payload was null for event: " + event));
                } else if (aClass.isAssignableFrom(event.payload.getClass())) {
                    subscriber.onNext((Event<T>) event);
                } else {
                    subscriber.onError(new ClassCastException("Invalid event payload type. Received " +
                            event.payload.getClass() + " but expected " + aClass));
                }
                break;
            }
            case ERROR: {
                subscriber.onError(Mappers.mapToThrowable(event.payload));
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

    private void sendCommandToExecutor(String commandName, Object arg, WebSocket webSocket) {
        final Command<?> command = Command.create(commandName, arg);
        final byte[] messageJson = messageToJsonBytes(command);
        webSocket.writeBinaryMessage(Buffer.buffer(messageJson));
    }
}
