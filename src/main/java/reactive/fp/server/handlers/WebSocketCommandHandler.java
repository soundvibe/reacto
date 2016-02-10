package reactive.fp.server.handlers;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;

import java.util.Objects;

/**
 * @author Linas on 2015.12.03.
 */
public class WebSocketCommandHandler implements Handler<ServerWebSocket> {

    private final CommandHandler commandHandler;

    public WebSocketCommandHandler(CommandHandler commandHandler) {
        Objects.requireNonNull(commandHandler, "Command Handler cannot be null");
        this.commandHandler = commandHandler;
    }

    @Override
    public void handle(ServerWebSocket serverWebSocket) {
        serverWebSocket.setWriteQueueMaxSize(Integer.MAX_VALUE);
        serverWebSocket.frameHandler(new WebSocketFrameHandler(buffer ->
                commandHandler.handle(buffer.getBytes(),
                        bytes -> serverWebSocket.writeBinaryMessage(Buffer.buffer(bytes)),
                        subscription -> serverWebSocket.closeHandler(event -> subscription.unsubscribe())
                )));
    }

}
