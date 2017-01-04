package net.soundvibe.reacto.server.handlers;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;

import java.util.Objects;

import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author Linas on 2015.12.03.
 */
public class WebSocketCommandHandler implements Handler<ServerWebSocket> {

    private final CommandHandler commandHandler;
    private final String root;

    public WebSocketCommandHandler(CommandHandler commandHandler, String root) {
        Objects.requireNonNull(commandHandler, "CommandHandler cannot be null");
        Objects.requireNonNull(root, "Root cannot be null");
        this.commandHandler = commandHandler;
        this.root = root;
    }

    @Override
    public void handle(ServerWebSocket serverWebSocket) {
        if (!canHandle(serverWebSocket.path())) {
            serverWebSocket.reject();
            return;
        }

        serverWebSocket.setWriteQueueMaxSize(Integer.MAX_VALUE);
        serverWebSocket.frameHandler(new WebSocketFrameHandler(buffer ->
                commandHandler.handle(buffer.getBytes(),
                        bytes -> serverWebSocket.writeBinaryMessage(Buffer.buffer(bytes)),
                        subscription -> serverWebSocket.closeHandler(event -> subscription.unsubscribe()),
                        serverWebSocket::close
                )));
    }

    private boolean canHandle(String path) {
        return root.equals(includeStartDelimiter(includeEndDelimiter(path)));
    }


}
