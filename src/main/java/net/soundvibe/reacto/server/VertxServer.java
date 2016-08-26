package net.soundvibe.reacto.server;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.discovery.DiscoverableServices;
import net.soundvibe.reacto.server.handlers.SSEHandler;
import net.soundvibe.reacto.server.handlers.WebSocketCommandHandler;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import net.soundvibe.reacto.server.handlers.CommandHandler;
import net.soundvibe.reacto.server.handlers.HystrixEventStreamHandler;
import net.soundvibe.reacto.utils.WebUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxServer implements Server {

    private static final Logger log = LoggerFactory.getLogger(VertxServer.class);

    private final String serviceName;
    private final String root;
    private final CommandRegistry commands;
    private final HttpServer httpServer;
    private final Router router;
    private final Optional<ServiceDiscovery> serviceDiscovery;
    private Record record;

    public VertxServer(String serviceName, Router router, HttpServer httpServer, String root, CommandRegistry commands,
                       Optional<ServiceDiscovery> serviceDiscovery) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        Objects.requireNonNull(router, "Router cannot be null");
        Objects.requireNonNull(httpServer, "HttpServer cannot be null");
        Objects.requireNonNull(root, "Root cannot be null");
        Objects.requireNonNull(commands, "CommandRegistry cannot be null");
        Objects.requireNonNull(serviceDiscovery, "ServiceDiscovery cannot be null");
        this.serviceName = serviceName;
        this.router = router;
        this.httpServer = httpServer;
        this.root = root;
        this.commands = commands;
        this.serviceDiscovery = serviceDiscovery;
    }

    @Override
    public void start() {
        setupRoutes();
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        httpServer.listen(event -> {
            if (event.succeeded()) {
                log.info("VertxServer has started successfully on port " + event.result().actualPort());

                if (!serviceDiscovery.isPresent()) {
                    countDownLatch.countDown();
                    return;
                }
                final ServiceDiscovery discovery = serviceDiscovery.get();
                record = createRecord(event.result().actualPort());
                DiscoverableServices.startHeartBeat(countDownLatch::countDown, record, discovery);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.info("Executing shutdown hook...");
                    DiscoverableServices.closeDiscovery(discovery, record);
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
                root());
    }

    @Override
    public void stop() {
        httpServer.close(event -> {
            if (event.succeeded()) {
                log.info("Server has stopped on port " + httpServer.actualPort());
                serviceDiscovery.ifPresent(discovery -> DiscoverableServices.closeDiscovery(discovery, record));
            }
        });
    }

    private void setupRoutes() {
        httpServer.websocketHandler(new WebSocketCommandHandler(new CommandHandler(commands)));
        router.route(root() + "hystrix.stream")
                .handler(new SSEHandler(HystrixEventStreamHandler::handle));
        httpServer.requestHandler(router::accept);
    }

    private String serviceName() {
        return WebUtils.excludeEndDelimiter(WebUtils.excludeStartDelimiter(serviceName));
    }

    private String root() {
        return WebUtils.includeEndDelimiter(WebUtils.includeStartDelimiter(root));
    }
}
