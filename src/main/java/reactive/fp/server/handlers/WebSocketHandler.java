package reactive.fp.server.handlers;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.http.WebSocketFrame;
import reactive.fp.server.CommandRegistry;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static reactive.fp.mappers.Mappers.fromJsonToCommand;
import static reactive.fp.mappers.Mappers.messageToJsonBytes;

/**
 * @author Linas on 2015.12.03.
 */
public class WebSocketHandler implements Handler<ServerWebSocket> {

    private final CommandRegistry commands;

    public WebSocketHandler(CommandRegistry commands) {
        this.commands = commands;
    }

    @Override
    public void handle(ServerWebSocket serverWebSocket) {
        commands.findCommand(getCommandNameFrom(serverWebSocket.path())).ifPresent(command -> serverWebSocket.handler(buffer -> {
            try {
                Command<?> receivedArgument = fromJsonToCommand(buffer.getBytes());
                final Subscription subscription = command.apply(receivedArgument.payload)
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                                payload -> send(serverWebSocket, Event.onNext(payload)),
                                throwable -> send(serverWebSocket, Event.onError(throwable)),
                                () -> send(serverWebSocket, Event.onCompleted("Completed")));
                serverWebSocket.closeHandler(event -> subscription.unsubscribe());
            } catch (Throwable e) {
                send(serverWebSocket, Event.onError(e));
            }
        }));
    }

    private void send(ServerWebSocket ws, Event<?> event) {
        final byte[] bytes = messageToJsonBytes(event);
        ws.writeFrame(WebSocketFrame.binaryFrame(Buffer.buffer(bytes), true));
    }

    private String getCommandNameFrom(String path) {
        return path.substring(path.lastIndexOf("/") + 1, path.length());
    }


}
