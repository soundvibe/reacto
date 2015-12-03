package reactive.fp.server.handlers;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller;
import io.vertx.core.http.HttpServerResponse;
import reactive.fp.server.handlers.SSEHandler;

/**
 * @author OZY on 2015.11.09.
 */
public interface HystrixEventStreamHandler {

    int DEFAULT_DELAY = 1000;

    static void handle(HttpServerResponse response) {
        final HystrixMetricsPoller hystrixMetricsPoller = new HystrixMetricsPoller(
                json -> SSEHandler.writeData(response, json), DEFAULT_DELAY);

        response.closeHandler(event -> hystrixMetricsPoller.shutdown());
        hystrixMetricsPoller.start();
    }
}
