package net.soundvibe.reacto.server.handlers;

import com.fasterxml.jackson.core.JsonFactory;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.*;
import net.soundvibe.reacto.metric.*;
import rx.Subscription;

import java.util.function.Consumer;

/**
 * @author OZY on 2017.01.12.
 */
public final class ReactoCommandMetricsStreamHandler implements Consumer<HttpServerResponse> {

    private static final Logger log = LoggerFactory.getLogger(ReactoCommandMetricsStreamHandler.class);

    private static final JsonFactory jsonFactory = new JsonFactory();

    @Override
    public void accept(HttpServerResponse httpServerResponse) {
        final Subscription subscription = ReactoDashboardStream.observeCommandHandlers()
                .subscribe(commandHandlerMetrics -> writeDashboardData(commandHandlerMetrics, httpServerResponse));

        httpServerResponse
                .exceptionHandler(error -> unsubscribeOnError(error, subscription))
                .closeHandler(__ -> unsubscribeOnClose(subscription));
    }

    private void writeDashboardData(CommandHandlerMetrics commandHandlerMetrics, HttpServerResponse response) {
        try {
            SSEHandler.writeData(response, getJson(commandHandlerMetrics));
        } catch (Throwable e) {
            log.error("Error when writing dashboard data: " + e);
        }
    }

    private String getJson(CommandHandlerMetrics commandHandlerMetrics) {
        //todo serialize to json string
        return "{}";
    }

    private void unsubscribeOnClose(Subscription subscription) {
        log.info("HttpResponse is closed so we are unsubscribing from the metrics stream");
        subscription.unsubscribe();
    }

    private void unsubscribeOnError(Throwable error, Subscription subscription) {
        log.error("HttpResponse Error: " + error);
        subscription.unsubscribe();
    }
}
