package reactive.fp.client.events;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import io.vertx.core.http.WebSocketStream;
import reactive.fp.mappers.Mappers;
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

    @SuppressWarnings("unchecked")
    private void checkForEvents(WebSocket webSocket, Subscriber<? super Event<T>> subscriber) {
        webSocket.handler(buffer -> {
            try {
                if (subscriber.isUnsubscribed()) return;
                final byte[] bytes = buffer.getBytes();
                final Event<?> receivedEvent = fromJsonToEvent(bytes);
                switch (receivedEvent.eventType) {
                    case NEXT: {
                        if (receivedEvent.payload == null) {
                            subscriber.onError(new NullPointerException("Payload was null for event: " + receivedEvent));
                        } else if (aClass.isAssignableFrom(receivedEvent.payload.getClass())) {
                            subscriber.onNext((Event<T>) receivedEvent);
                        } else {
                            subscriber.onError(new ClassCastException("Invalid event payload type. Received " +
                                    receivedEvent.payload.getClass() + " but expected " + aClass));
                        }
                        break;
                    }
                    case ERROR: {
                        subscriber.onError(Mappers.mapToThrowable(receivedEvent.payload));
                        break;
                    }
                    case COMPLETED: {
                        subscriber.onCompleted();
                        break;
                    }
                }
            } catch (Throwable e) {
                subscriber.onError(e);
            }
        });
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
                webSocketStream.handler(webSocket -> {
                    try {
                        executeCommand(commandName, arg, webSocket);
                        checkForEvents(webSocket, subscriber);
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }).exceptionHandler(subscriber::onError).endHandler(e -> subscriber.onCompleted());
            } catch (Throwable e) {
                subscriber.onError(e);
            }
        });
    }

    private int getPortFromURI(URI uri) {
        return uri.getPort() == -1 ?
                80:
                uri.getPort();
    }

    private void executeCommand(String commandName, Object arg, WebSocket webSocket) {
        final Command<?> command = Command.create(commandName, arg);
        final byte[] messageJson = messageToJsonBytes(command);
        webSocket.writeFinalBinaryFrame(Buffer.buffer(messageJson));
    }

}
