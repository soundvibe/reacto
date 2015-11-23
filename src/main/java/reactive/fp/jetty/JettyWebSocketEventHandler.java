package reactive.fp.jetty;

import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import reactive.fp.types.EventHandler;
import rx.Observable;
import rx.subjects.ReplaySubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import java.net.URI;

/**
 * @author OZY on 2015.11.13.
 */
public class JettyWebSocketEventHandler<T,U> implements AutoCloseable, EventHandler<T,U> {

    private final URI wsUrl;
    private final JettyWebSocketClient jettyWebSocketClient;
    private final Subject<U, U> subject;
    private final WebSocketClient client;

    public JettyWebSocketEventHandler(URI wsUrl) {
        this.wsUrl = wsUrl;
        this.client = new WebSocketClient();
        this.jettyWebSocketClient = new JettyWebSocketClient(this::handleEvent);
        this.subject = new SerializedSubject<>(ReplaySubject.create());
    }

    public void start() {
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        try {
            client.start();
            client.connect(jettyWebSocketClient, wsUrl, request).get();
        } catch (Exception e) {
            throw new RuntimeException("Cannot start WebSocket client.", e);
        }
    }

    @Override
    public void close() {
        jettyWebSocketClient.close();
        try {
            client.stop();
        } catch (Exception e) {
            throw new RuntimeException("Cannot stop WebSocketClient. ", e);
        }
    }

    protected void handleEvent(Event<?> event) {
        if (event == null) return;
        switch (event.eventType) {
            case NEXT: {
                if (hasNotTerminated()) {
                    subject.onNext(mapFromEvent(event));
                }
                break;
            }
            case ERROR: {
                if (hasNotTerminated()) {
                    subject.onError((Throwable) event.payload);
                }
                break;
            }
            case COMPLETED: {
                if (hasNotTerminated()) {
                    subject.onCompleted();
                }
                break;
            }
        }
    }

    private boolean hasNotTerminated() {
        return !subject.hasThrowable() && !subject.hasCompleted();
    }

    @SuppressWarnings("unchecked")
    protected U mapFromEvent(Event<?> event) {
        return (U) event.payload;
    }

    public Observable<U> toObservable(String commandName, T arg) {
        jettyWebSocketClient.publishMessage(Command.create(commandName, arg));
        return subject;
    }
}
