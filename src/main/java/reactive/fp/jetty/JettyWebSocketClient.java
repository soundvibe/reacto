package reactive.fp.jetty;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import reactive.fp.mappers.Mappers;
import reactive.fp.types.Event;
import reactive.fp.types.Message;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * @author OZY on 2015.11.13.
 */
@WebSocket
public class JettyWebSocketClient {

    private AtomicReference<Session> session = new AtomicReference<>();
    private final Consumer<Event<?>> messageConsumer;

    public JettyWebSocketClient(Consumer<Event<?>> messageConsumer) {
        this.messageConsumer = messageConsumer;
    }

    public boolean isConnected() {
        return session.get() != null && session.get().isOpen();
    }

    @OnWebSocketConnect
    public void connected(Session session) {
        this.session.set(session);
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        this.session.set(null);
    }

    @OnWebSocketMessage
    public synchronized void message(Session session, String message) throws IOException {
        System.out.println("Received Message: " + message);
        Event<?> receivedEvent = Mappers.jsonMapper.readValue(message, Event.class);
        if (receivedEvent != null) {
            messageConsumer.accept(receivedEvent);
        }
    }

    @OnWebSocketError
    public void error(Session session, Throwable error) {
        this.session.set(null);
    }

    public void close() {
        Session session = this.session.get();
        if (session != null) {
            session.close();
        }
    }

    public void publishMessage(Message<?> message) {
        String messageJson = null;
        try {
            messageJson = Mappers.jsonMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Cannot serialize message to json: " + message);
        }
        publish(messageJson);
    }

    public void publish(String message) {
        if (session.get() != null && message != null) {
            try {
                session.get().getRemote().sendString(message);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
