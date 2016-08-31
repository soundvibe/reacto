package net.soundvibe.reacto.server;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.discovery.DiscoverableService;
import net.soundvibe.reacto.server.handlers.*;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import net.soundvibe.reacto.utils.WebUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxServer implements Server {

    public static final int INTERNAL_SERVER_ERROR = 500;
    private static final Logger log = LoggerFactory.getLogger(VertxServer.class);

    private final ServiceOptions serviceOptions;
    private final CommandRegistry commands;
    private final HttpServer httpServer;
    private final Router router;
    private Record record;

    public VertxServer(ServiceOptions serviceOptions, Router router, HttpServer httpServer, CommandRegistry commands) {
        Objects.requireNonNull(serviceOptions, "serviceOptions cannot be null");
        Objects.requireNonNull(router, "Router cannot be null");
        Objects.requireNonNull(httpServer, "HttpServer cannot be null");
        Objects.requireNonNull(commands, "CommandRegistry cannot be null");
        this.serviceOptions = serviceOptions;
        this.router = router;
        this.httpServer = httpServer;
        this.commands = commands;
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Override
    public void start() {
        setupRoutes();
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        httpServer.listen(event -> {
            if (event.succeeded()) {
                log.info("VertxServer has started successfully on port " + event.result().actualPort());

                if (!serviceOptions.serviceDiscovery.isPresent()) {
                    countDownLatch.countDown();
                    return;
                }
                final DiscoverableService discovery = serviceOptions.serviceDiscovery.get();
                record = createRecord(event.result().actualPort());
                discovery.startDiscovery(record)
                        .doOnCompleted(() -> discovery.startHeartBeat(record))
                        .doOnTerminate(countDownLatch::countDown)
                        .subscribe();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.info("Executing shutdown hook...");
                    discovery.closeDiscovery(record).subscribe();
                }));
            }
            if (event.failed()) {
                log.error("Error when starting the server: " + event.cause(), event.cause());
                countDownLatch.countDown();
            }
        });
        try {
            countDownLatch.await(1L, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Record createRecord(int port) {
        return HttpEndpoint.createRecord(
                serviceName(),
                WebUtils.getLocalAddress(),
                port,
                root(),
                new JsonObject().put("version", serviceOptions.version));
    }

    @Override
    public void stop() {
        httpServer.close(event -> {
            if (event.succeeded()) {
                log.info("Server has stopped on port " + httpServer.actualPort());
                serviceOptions.serviceDiscovery.ifPresent(discovery -> discovery.closeDiscovery(record).subscribe());
            }
        });
    }

    private void setupRoutes() {
        httpServer.websocketHandler(new WebSocketCommandHandler(new CommandHandler(commands)));
        router.route(root() + "hystrix.stream")
                .handler(new SSEHandler(HystrixEventStreamHandler::handle));

        serviceOptions.serviceDiscovery.ifPresent(discovery ->
                router.route(root() + "service-discovery/:action")
                        .produces("application/json")
                        .handler(new ServiceDiscoveryHandler(discovery, () -> record)));
        httpServer.requestHandler(router::accept);
    }

    private String serviceName() {
        return WebUtils.excludeEndDelimiter(WebUtils.excludeStartDelimiter(serviceOptions.serviceName));
    }

    private String root() {
        return WebUtils.includeEndDelimiter(WebUtils.includeStartDelimiter(serviceOptions.root));
    }
}
