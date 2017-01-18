package net.soundvibe.reacto.server.vertx;

import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.*;
import io.vertx.ext.web.Router;
import net.soundvibe.reacto.discovery.ServiceDiscoveryLifecycle;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.server.*;
import net.soundvibe.reacto.server.vertx.handlers.*;
import net.soundvibe.reacto.types.Any;
import rx.Observable;

import java.util.Objects;

import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxServer implements Server<HttpServer> {

    public static final int INTERNAL_SERVER_ERROR = 500;

    private static final Logger log = LoggerFactory.getLogger(VertxServer.class);

    public static final String HYSTRIX_STREAM_PATH = "hystrix.stream";
    public static final String REACTO_STREAM_PATH = "reacto.stream";

    private final ServiceOptions serviceOptions;
    private final CommandRegistry commands;
    private final HttpServer httpServer;
    private final Router router;
    private final ServiceDiscoveryLifecycle discoveryLifecycle;

    public VertxServer(
            ServiceOptions serviceOptions,
            Router router,
            HttpServer httpServer,
            CommandRegistry commands,
            ServiceDiscoveryLifecycle discoveryLifecycle) {
        Objects.requireNonNull(serviceOptions, "serviceOptions cannot be null");
        Objects.requireNonNull(router, "Router cannot be null");
        Objects.requireNonNull(httpServer, "HttpServer cannot be null");
        Objects.requireNonNull(commands, "CommandRegistry cannot be null");
        Objects.requireNonNull(discoveryLifecycle, "discoveryLifecycle cannot be null");
        this.serviceOptions = serviceOptions;
        this.router = router;
        this.httpServer = httpServer;
        this.commands = commands;
        this.discoveryLifecycle = discoveryLifecycle;
    }

    @Override
    public Observable<HttpServer> start() {
        return Observable.<HttpServer>create(subscriber -> {
            subscriber.onStart();
            setupRoutes();
            httpServer.listen(event -> {
                if (event.succeeded()) {
                    log.info("VertxServer has started successfully on port " + event.result().actualPort());
                    subscriber.onNext(event.result());
                    subscriber.onCompleted();
                }
                if (event.failed()) {
                    log.error("Error when starting the server: " + event.cause(), event.cause());
                    subscriber.onError(event.cause());
                }
            });
        }).flatMap(server -> discoveryLifecycle.startDiscovery(
                getHttpEndpoint(server), commands))
                .map(r -> httpServer);
    }

    private ServiceRecord getHttpEndpoint(HttpServer server) {
        return ServiceRecord.createHttpEndpoint(
                serviceName(),
                server.actualPort(),
                root(),
                serviceOptions.version);
    }

    @Override
    public Observable<Any> stop() {
        return Observable.create(subscriber ->
            httpServer.close(event -> {
                if (event.succeeded()) {
                    log.info("Server has stopped on port " + httpServer.actualPort());
                    if (!subscriber.isUnsubscribed()) {
                        subscriber.onNext(Any.VOID);
                        subscriber.onCompleted();
                    }
                }
                if (event.failed() && !subscriber.isUnsubscribed()) {
                    subscriber.onError(event.cause());
                }
            })).flatMap(__ -> discoveryLifecycle.closeDiscovery());
    }

    private void setupRoutes() {
        httpServer.websocketHandler(new WebSocketCommandHandler(new CommandProcessor(commands), root()));
        router.route(root() + HYSTRIX_STREAM_PATH)
            .handler(new SSEHandler(HystrixEventStreamHandler::handle));

        router.route(root() + REACTO_STREAM_PATH)
                .handler(new SSEHandler(new ReactoCommandMetricsStreamHandler()));

        router.route(root() + "service-discovery/:action")
            .produces("application/json")
            .handler(new ServiceDiscoveryHandler(discoveryLifecycle, () -> getHttpEndpoint(httpServer), commands));
        httpServer.requestHandler(router::accept);
    }

    private String serviceName() {
        return excludeEndDelimiter(excludeStartDelimiter(serviceOptions.serviceName));
    }

    private String root() {
        return includeEndDelimiter(includeStartDelimiter(serviceOptions.root));
    }

}
