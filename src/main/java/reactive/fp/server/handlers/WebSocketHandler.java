package reactive.fp.server.handlers;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import reactive.fp.mappers.Mappers;
import reactive.fp.server.CommandRegistry;
import reactive.fp.types.Command;
import reactive.fp.internal.InternalEvent;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static reactive.fp.mappers.Mappers.fromBytesToCommand;

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
        serverWebSocket.setWriteQueueMaxSize(Integer.MAX_VALUE);
        commands.findCommand(getCommandNameFrom(serverWebSocket.path())).ifPresent(command ->
                serverWebSocket.frameHandler(new WebSocketFrameHandler(buffer -> {
                    try {
                        Command receivedCommand = fromBytesToCommand(buffer.getBytes());
                        final Subscription subscription = command.apply(receivedCommand)
                                .subscribeOn(Schedulers.computation())
                                .subscribe(
                                        event -> send(serverWebSocket, InternalEvent.onNext(event)),
                                        throwable -> send(serverWebSocket, InternalEvent.onError(throwable)),
                                        () -> send(serverWebSocket, InternalEvent.onCompleted()));
                        serverWebSocket.closeHandler(event -> subscription.unsubscribe());
                    } catch (Throwable e) {
                        send(serverWebSocket, InternalEvent.onError(e));
                    }

                })));
    }

    private void send(ServerWebSocket ws, InternalEvent internalEvent) {
        final byte[] bytes = Mappers.internalEventToBytes(internalEvent);
        ws.writeBinaryMessage(Buffer.buffer(bytes));
    }

    private String getCommandNameFrom(String path) {
        return path.substring(path.lastIndexOf("/") + 1, path.length());
    }


}
