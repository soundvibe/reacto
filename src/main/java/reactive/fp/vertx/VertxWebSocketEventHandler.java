package reactive.fp.vertx;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.WebSocket;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import reactive.fp.types.EventHandler;
import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import java.net.URI;

import static reactive.fp.mappers.Mappers.fromJsonToEvent;
import static reactive.fp.mappers.Mappers.messageToJsonString;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxWebSocketEventHandler<T,U> implements EventHandler<T,U> {

    private final URI wsUrl;
    private final Subject<U, U> subject;
    private final Vertx vertx;

    public VertxWebSocketEventHandler(URI wsUrl) {
        this.wsUrl = wsUrl;
        this.vertx = Vertx.vertx();
        this.subject = new SerializedSubject<>(ReplaySubject.create());
    }

    protected void checkForEvents(WebSocket webSocket) {
        webSocket.handler(buffer -> {
                    byte[] bytes = buffer.getBytes();
                    Event<?> receivedEvent = fromJsonToEvent(bytes);
                    switch (receivedEvent.eventType) {
                        case NEXT: {
                            subject.onNext(mapFromEvent(receivedEvent));
                            break;
                        }
                        case ERROR: {
                            subject.onError((Throwable) receivedEvent.payload);
                            break;
                        }
                        case COMPLETED: {
                            subject.onCompleted();
                            break;
                        }
                    }
                }
        );
    }

    @SuppressWarnings("unchecked")
    protected U mapFromEvent(Event<?> event) {
        return (U) event.payload;
    }

    public Observable<U> toObservable(String commandName, T arg) {
        final HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions());
        return Observable.using(() -> httpClient.websocketStream(wsUrl.getPort() == -1 ? 80: wsUrl.getPort(), wsUrl.getHost(), wsUrl.getPath()),
                webSocketStream -> {
                    webSocketStream.handler(webSocket -> {
                        startCommand(commandName, arg, webSocket);
                        checkForEvents(webSocket);
                    });
                    return subject;
                }, webSocketStream -> {webSocketStream.pause(); httpClient.close();});
    }

    private void startCommand(String commandName, T arg, WebSocket webSocket) {
        Command<T> command = Command.create(commandName, arg);
        String messageJson = messageToJsonString(command);
        webSocket.writeFinalTextFrame(messageJson);
    }

}
