package net.soundvibe.reacto.client.events;

import io.vertx.core.Vertx;
import io.vertx.core.http.*;
import io.vertx.core.logging.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * @author OZY on 2017.01.13.
 */
public final class VertxEventSource implements EventSource {

    private static final Logger log = LoggerFactory.getLogger(VertxEventSource.class);
    public static final String DATA_PREFIX = "data: ";

    private final Vertx vertx;
    private final String url;
    private HttpClient httpClient;
    private final List<Runnable> onOpenListeners = new ArrayList<>();
    private final List<Consumer<Throwable>> onErrorListeners = new ArrayList<>();
    private final List<Consumer<String>> onMessageListeners = new ArrayList<>();

    public VertxEventSource(Vertx vertx, String url) {
        Objects.requireNonNull(vertx, "vertx cannot be null");
        Objects.requireNonNull(url, "url cannot be null");
        this.vertx = vertx;
        this.url = url;
    }

    @Override
    public VertxEventSource onOpen(Runnable runnable) {
        onOpenListeners.add(runnable);
        return this;
    }

    @Override
    public VertxEventSource onError(Consumer<Throwable> errorConsumer) {
        onErrorListeners.add(errorConsumer);
        return this;
    }

    @Override
    public VertxEventSource onMessage(Consumer<String> messageConsumer) {
        onMessageListeners.add(messageConsumer);
        return this;
    }

    @Override
    public void open() {
        if (httpClient != null) throw new IllegalStateException("EventSource already open");
        httpClient = vertx.createHttpClient(
                new HttpClientOptions()
                        .setTcpKeepAlive(true)
                        .setReceiveBufferSize(1024 * 1000 * 32)
                        .setKeepAlive(true)
                        .setMaxChunkSize(1024 * 1000 * 32)
        );

        final HttpClientRequest httpClientRequest = httpClient.getAbs(url, this::handleResponse);
        httpClientRequest
                .exceptionHandler(this::handleError)
                .endHandler(__ -> close())
                .connectionHandler(this::handleConnectionOpen)
                .setChunked(true)
                .putHeader("Accept", "text/event-stream")
                .end();
    }

    private void handleResponse(HttpClientResponse response) {
        response.handler(buffer -> {
            log.info("Received response: " + buffer.length());
            final String message = buffer.toString();
            if (message != null && message.startsWith(DATA_PREFIX)) {
                final String data = message.substring(DATA_PREFIX.length());
                onMessageListeners.forEach(msgConsumer -> msgConsumer.accept(data));
            }
        });
    }

    private void handleError(Throwable error) {
        log.error("EventSource error: " + error);
        onErrorListeners.forEach(throwableConsumer -> throwableConsumer.accept(error));
        close();
    }

    private void handleConnectionOpen(HttpConnection httpConnection) {
        log.info("Connection established");
        onOpenListeners.forEach(Runnable::run);
    }

    public void close() {
        if (httpClient != null) {
            httpClient.close();
            log.info("EventSource closed");
            httpClient = null;
        }
    }

}
