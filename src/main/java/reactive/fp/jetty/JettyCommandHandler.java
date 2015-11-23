package reactive.fp.jetty;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import reactive.fp.mappers.Mappers;
import reactive.fp.commands.CommandRegistry;
import reactive.fp.types.Command;
import reactive.fp.types.Event;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Linas on 2015.11.12.
 */
@WebSocket
public class JettyCommandHandler {

    private static final Queue<Session> sessions = new ConcurrentLinkedQueue<>();

    //todo inject CommandRegistry here when SparkJava fixes issue with webSockets

    @OnWebSocketMessage
    public void message(Session session, String message) throws IOException {
        Command<?> receivedCommand = Mappers.jsonMapper.readValue(message, Command.class);
        Optional.ofNullable(CommandRegistry.commands.get(getCommandNameFrom(session)))
                .filter(m -> receivedCommand != null)
                .map(command -> command.apply(receivedCommand.payload))
                .ifPresent(observable -> observable
                        .subscribe(
                                payload -> send(session, Event.onNext(payload)),
                                throwable -> send(session, Event.onError(throwable)),
                                () -> send(session, Event.onCompleted("Completed")))
                );
        ;
    }

    @OnWebSocketConnect
    public void connected(Session session) {
        sessions.add(session);
    }

    @OnWebSocketClose
    public void closed(Session session, int statusCode, String reason) {
        sessions.remove(session);
    }

    public void send(Session session, Event<?> event) {
        try {
            final String value = Mappers.jsonMapper.writeValueAsString(event);
            session.getRemote().sendString(value);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void publish(String message) {
        sessions.forEach(session -> {
            try {
                session.getRemote().sendString(message);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    protected String getCommandNameFrom(Session session) {
        String path = session.getUpgradeRequest().getRequestURI().getPath();
        return path.substring(path.lastIndexOf("/") + 1, path.length());
    }

}
