package reactive.fp.client.events;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import reactive.fp.mappers.Mappers;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import reactive.fp.utils.Factories;
import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import java.net.URI;
import java.util.Objects;

import static reactive.fp.mappers.Mappers.fromJsonToEvent;
import static reactive.fp.mappers.Mappers.messageToJsonBytes;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxWebSocketEventHandler<T> implements EventHandler<T> {

    private final URI wsUrl;
    private final Subject<Event<T>, Event<T>> subject;
    private final Vertx vertx;
    private final Class<?> aClass;

    public VertxWebSocketEventHandler(URI wsUrl, Class<?> eventClass) {
        Objects.requireNonNull(wsUrl, "WebSocket URI cannot be null");
        Objects.requireNonNull(eventClass, "Event class cannot be null");
        this.wsUrl = wsUrl;
        this.aClass = eventClass;
        this.vertx = Factories.vertx();
        this.subject = new SerializedSubject<>(ReplaySubject.create());
    }

    @SuppressWarnings("unchecked")
    private void checkForEvents(WebSocket webSocket) {
        webSocket.handler(buffer -> {
            try {
                final byte[] bytes = buffer.getBytes();
                final Event<?> receivedEvent = fromJsonToEvent(bytes);
                switch (receivedEvent.eventType) {
                    case NEXT: {
                        if (receivedEvent.payload == null) {
                            subject.onError(new NullPointerException("Payload was null for event: " + receivedEvent));
                        } else if (aClass.isAssignableFrom(receivedEvent.payload.getClass())) {
                            subject.onNext((Event<T>) receivedEvent);
                        } else {
                            subject.onError(new ClassCastException("Invalid event payload type. Received " +
                                    receivedEvent.payload.getClass() + " but expected " + aClass));
                        }
                        break;
                    }
                    case ERROR: {
                        subject.onError(Mappers.mapToThrowable(receivedEvent.payload));
                        break;
                    }
                    case COMPLETED: {
                        subject.onCompleted();
                        break;
                    }
                }
            } catch (Throwable e) {
                subject.onError(e);
            }
        }
        );
    }

    public Observable<Event<T>> toObservable(String commandName, Object arg) {
        final HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions());
        return Observable.using(() -> httpClient.websocketStream(wsUrl.getPort() == -1 ?
                80:
                wsUrl.getPort(), wsUrl.getHost(), wsUrl.getPath()),
                webSocketStream -> {
                    webSocketStream.handler(webSocket -> {
                        try {
                            startCommand(commandName, arg, webSocket);
                            checkForEvents(webSocket);
                        } catch (Throwable e) {
                            subject.onError(e);
                        }
                    });
                    return subject;
                }, webSocketStream -> { webSocketStream.pause(); httpClient.close(); });
    }

    private void startCommand(String commandName, Object arg, WebSocket webSocket) {
        final Command<?> command = Command.create(commandName, arg);
        final byte[] messageJson = messageToJsonBytes(command);
        webSocket.writeFinalBinaryFrame(Buffer.buffer(messageJson));
    }

}
