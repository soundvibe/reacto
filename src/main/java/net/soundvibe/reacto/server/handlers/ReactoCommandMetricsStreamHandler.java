package net.soundvibe.reacto.server.handlers;

import com.fasterxml.jackson.core.*;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.logging.*;
import net.soundvibe.reacto.metric.*;
import rx.Subscription;

import java.io.*;
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
                .subscribe(commandHandlerMetrics -> writeDashboardData(commandHandlerMetrics, httpServerResponse),
                        throwable -> log.error("Error when getting command metrics data: " + throwable));

        httpServerResponse
                .exceptionHandler(error -> unsubscribeOnError(error, subscription))
                .closeHandler(__ -> unsubscribeOnClose(subscription));
    }

    private void writeDashboardData(CommandProcessorMetrics commandProcessorMetrics, HttpServerResponse response) {
        try {
            SSEHandler.writeData(response, getJson(commandProcessorMetrics));
        } catch (Throwable e) {
            log.error("Error when writing dashboard data: " + e);
        }
    }

    public static String getJson(CommandProcessorMetrics metrics) {
        try (StringWriter jsonString = new StringWriter()) {
            final JsonGenerator json = jsonFactory.createGenerator(jsonString);
            json.writeStartObject();

                json.writeObjectFieldStart("memoryUsage");
                json.writeNumberField("committed", metrics.memoryUsage().getCommitted());
                json.writeNumberField("init", metrics.memoryUsage().getInit());
                json.writeNumberField("max", metrics.memoryUsage().getMax());
                json.writeNumberField("used", metrics.memoryUsage().getUsed());
                json.writeEndObject();

                json.writeArrayFieldStart("commands");
                for (CommandProcessorMetric commandMetric : metrics.commands()) {
                    json.writeStartObject();
                    json.writeStringField("commandName", commandMetric.commandName());
                    json.writeStringField("eventName", commandMetric.eventName());
                    json.writeNumberField("eventCount", commandMetric.eventCount());
                    json.writeNumberField("commandCount", commandMetric.commandCount());
                    json.writeNumberField("totalExecutionTimeInMs", commandMetric.totalExecutionTimeInMs());
                    json.writeNumberField("completed", commandMetric.completed());
                    json.writeNumberField("errors", commandMetric.errors());
                    json.writeEndObject();
                }
                json.writeEndArray();

            json.writeEndObject();
            json.close();
            return jsonString.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
