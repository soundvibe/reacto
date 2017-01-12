package net.soundvibe.reacto.server.handlers;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.logging.*;
import net.soundvibe.reacto.internal.InternalEvent;
import net.soundvibe.reacto.server.*;
import rx.Subscription;

import java.util.Objects;

import static net.soundvibe.reacto.mappers.Mappers.internalEventToBytes;
import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author Linas on 2015.12.03.
 */
public class WebSocketCommandHandler implements Handler<ServerWebSocket> {

    private final CommandProcessor commandProcessor;
    private final String root;

    private static final Logger log = LoggerFactory.getLogger(WebSocketCommandHandler.class);

    public WebSocketCommandHandler(CommandProcessor commandProcessor, String root) {
        Objects.requireNonNull(commandProcessor, "CommandProcessor cannot be null");
        Objects.requireNonNull(root, "Root cannot be null");
        this.commandProcessor = commandProcessor;
        this.root = root;
    }

    @Override
    public void handle(ServerWebSocket serverWebSocket) {
        if (!shouldHandle(serverWebSocket.path())) {
            serverWebSocket.reject();
            return;
        }

        serverWebSocket
            .setWriteQueueMaxSize(Integer.MAX_VALUE)
            .frameHandler(new WebSocketFrameHandler(buffer -> {
                final Subscription subscription = commandProcessor.process(buffer.getBytes())
                        .map(event -> internalEventToBytes(InternalEvent.onNext(event)))
                        .subscribe(
                                bytes -> writeOnNext(bytes, serverWebSocket),
                                error -> writeOnError(error, serverWebSocket),
                                () -> writeOnCompleted(serverWebSocket)
                        );
                serverWebSocket
                        .exceptionHandler(exception -> {
                            log.error("ServerWebSocket exception: " + exception);
                            subscription.unsubscribe();
                        })
                        .closeHandler(__ -> subscription.unsubscribe());
            }));
    }

    private void writeOnNext(byte[] bytes, ServerWebSocket serverWebSocket) {
        serverWebSocket.writeBinaryMessage(Buffer.buffer(bytes));
    }

    private void writeOnError(Throwable error, ServerWebSocket serverWebSocket) {
        serverWebSocket.writeBinaryMessage(Buffer.buffer(internalEventToBytes(InternalEvent.onError(error))));
        serverWebSocket.close();
    }

    private void writeOnCompleted(ServerWebSocket serverWebSocket) {
        serverWebSocket.writeBinaryMessage(Buffer.buffer(internalEventToBytes(InternalEvent.onCompleted())));
        serverWebSocket.close();
    }

    private boolean shouldHandle(String path) {
        return root.equals(includeStartDelimiter(includeEndDelimiter(path)));
    }


}
