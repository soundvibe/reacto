package reactive.fp.server.handlers;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;
import reactive.fp.types.Event;

import java.util.function.Consumer;

import static reactive.fp.mappers.Mappers.messageToJsonString;

/**
 * @author Linas on 2015.12.03.
 */
public class SSEHandler implements Handler<RoutingContext> {

    private final Consumer<HttpServerResponse> responseConsumer;

    private volatile HttpServerResponse response;

    public SSEHandler(Consumer<HttpServerResponse> responseConsumer) {
        this.responseConsumer = responseConsumer;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.setAcceptableContentType("text/event-stream");
        response = routingContext.response();
        response.putHeader("Content-Type", "text/event-stream");
        response.putHeader("Connection", "keep-alive");
        response.putHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.putHeader("Content-Encoding", "UTF-8");
        response.putHeader("Pragma", "no-cache");
        response.setChunked(true);
        responseConsumer.accept(response);
    }

    public void write(String data) {
        writeData(response, data);
    }

    public void writeEvent(Event<?> event) {
        final String json = messageToJsonString(event);
        write(json);
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
