package net.soundvibe.reacto.server.handlers;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Linas on 2015.12.03.
 */
public class SSEHandler implements Handler<RoutingContext> {

    private final Consumer<HttpServerResponse> responseConsumer;
    private volatile HttpServerResponse response;

    public SSEHandler(Consumer<HttpServerResponse> responseConsumer) {
        Objects.requireNonNull(responseConsumer, "responseConsumer cannot be null");
        this.responseConsumer = responseConsumer;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext
                .setAcceptableContentType("text/event-stream");
        response = routingContext.response();
        response.putHeader("Content-Type", "text/event-stream")
                .putHeader("Connection", "keep-alive")
                .putHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate")
                .putHeader("Content-Encoding", "UTF-8")
                .putHeader("Pragma", "no-cache")
                .setChunked(true);
        responseConsumer.accept(response);
    }

    public void write(String data) {
        writeData(response, data);
    }

    public static void writeData(HttpServerResponse response, String data) {
        if (response == null) return;
        if (data == null || "".equals(data)) {
            response.write("ping: \n\n");
        } else {
            response.write("data: " + data + "\n\n");
        }
    }
}
