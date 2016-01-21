package reactive.fp.server.handlers;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketFrame;

import java.util.function.Consumer;

/**
 * @author OZY on 2016.01.21.
 */
public class WebSocketFrameHandler implements io.vertx.core.Handler<WebSocketFrame> {

    private final Consumer<Buffer> bufferConsumer;
    private Buffer buffer = Buffer.buffer();

    public WebSocketFrameHandler(Consumer<Buffer> bufferConsumer) {
        this.bufferConsumer = bufferConsumer;
    }

    @Override
    public void handle(WebSocketFrame frame) {
        buffer.appendBuffer(frame.binaryData());
        if (frame.isFinal()) {
            try {
                bufferConsumer.accept(buffer);
            } finally {
                buffer = null;
                buffer = Buffer.buffer();
            }
        }
    }
}
