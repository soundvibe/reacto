package reactive.fp.vertx;

import com.netflix.hystrix.contrib.metrics.eventstream.HystrixMetricsPoller;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.RoutingContext;

/**
 * @author OZY on 2015.11.09.
 */
public class HystrixEventStreamHandler implements Handler<RoutingContext> {

    static int DEFAULT_DELAY = 1000;
    private final int delay;

    public HystrixEventStreamHandler() {
        this.delay = DEFAULT_DELAY;
    }

    public HystrixEventStreamHandler(int delay) {
        this.delay = delay;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        routingContext.setAcceptableContentType("text/event-stream");
        HttpServerResponse response = routingContext.response();
        response.putHeader("Content-Type", "text/event-stream");
        response.putHeader("Connection", "keep-alive");
        response.putHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        response.putHeader("Content-Encoding", "UTF-8");
        response.putHeader("Pragma", "no-cache");
        response.setChunked(true);
        final HystrixMetricsPoller hystrixMetricsPoller = new HystrixMetricsPoller(json -> {
            if (json == null || "".equals(json)) {
                response.write("ping: \n\n");
            } else {
                response.write("data: " + json + "\n\n");
            }
        }, delay);
        hystrixMetricsPoller.start();
    }

}
