package net.soundvibe.reacto.server.handlers;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.WebSocketFrame;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * @author OZY on 2016.01.21.
 */
public class WebSocketFrameHandler implements io.vertx.core.Handler<WebSocketFrame> {

    private final Consumer<Buffer> bufferConsumer;
    private final ConcurrentHashMap<Long, Buffer> buffers = new ConcurrentHashMap<>();


    public WebSocketFrameHandler(Consumer<Buffer> bufferConsumer) {
        this.bufferConsumer = bufferConsumer;
    }

    @Override
    public void handle(WebSocketFrame frame) {
        final Buffer buffer = buffers.merge(Thread.currentThread().getId(), createBuffer(frame),
                Buffer::appendBuffer);
        if (frame.isFinal()) {
            bufferConsumer.accept(buffer);
            buffers.remove(Thread.currentThread().getId());
        }
    }

    private Buffer createBuffer(WebSocketFrame frame) {
        final Buffer buffer = Buffer.buffer();
        buffer.appendBuffer(frame.binaryData());
        return buffer;
    }
}
