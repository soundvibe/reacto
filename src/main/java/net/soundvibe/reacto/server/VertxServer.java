package net.soundvibe.reacto.server;

import io.vertx.core.http.HttpServer;
import io.vertx.core.json.*;
import io.vertx.core.logging.*;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.discovery.DiscoverableService;
import net.soundvibe.reacto.server.handlers.*;
import net.soundvibe.reacto.types.Pair;
import net.soundvibe.reacto.utils.WebUtils;
import rx.Observable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static net.soundvibe.reacto.server.ServiceRecords.COMMANDS;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxServer implements Server<HttpServer> {

    public static final int INTERNAL_SERVER_ERROR = 500;
    private static final Logger log = LoggerFactory.getLogger(VertxServer.class);

    private final ServiceOptions serviceOptions;
    private final CommandRegistry commands;
    private final HttpServer httpServer;
    private final Router router;
    private final AtomicReference<Record> record = new AtomicReference<>();
    private final JsonObject metadataJson;

    public VertxServer(ServiceOptions serviceOptions, Router router, HttpServer httpServer, CommandRegistry commands) {
        Objects.requireNonNull(serviceOptions, "serviceOptions cannot be null");
        Objects.requireNonNull(router, "Router cannot be null");
        Objects.requireNonNull(httpServer, "HttpServer cannot be null");
        Objects.requireNonNull(commands, "CommandRegistry cannot be null");
        this.serviceOptions = serviceOptions;
        this.router = router;
        this.httpServer = httpServer;
        this.commands = commands;
        this.metadataJson = createMetadata();
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
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
        }).flatMap(server ->
                serviceOptions.serviceDiscovery.isPresent() ?
                    Observable.just(serviceOptions.serviceDiscovery.get())
                        .flatMap(discoverableService ->
                                discoverableService.startDiscovery(createRecord(server.actualPort()))
                                    .doOnNext(discoverableService::startHeartBeat)
                                    .doOnNext(rec -> Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                                        log.info("Executing shutdown hook...");
                                        discoverableService.closeDiscovery(rec).subscribe(
                                                r -> log.debug("Service discovery closed successfully"),
                                                e -> log.debug("Error when closing service discovery: " + e)
                                        );
                                    })))
                                    .doOnNext(record::set))
                        .map(__ -> httpServer) :
                Observable.just(server));
    }

    private Record createRecord(int port) {
        return HttpEndpoint.createRecord(
                serviceName(),
                WebUtils.getLocalAddress(),
                port,
                root(),
                metadataJson
        );
    }

    private JsonObject createMetadata() {
        return new JsonObject()
                .put("version", serviceOptions.version)
                .put(COMMANDS, commandsToJsonArray(commands));
    }

    static JsonArray commandsToJsonArray(CommandRegistry commands) {
        return commands.stream()
                .map(Pair::getKey)
                .reduce(new JsonArray(), JsonArray::add, JsonArray::addAll);
    }

    @Override
    public Observable<Void> stop() {
        return Observable.<DiscoverableService>create(subscriber ->
            httpServer.close(event -> {
                if (event.succeeded()) {
                    log.info("Server has stopped on port " + httpServer.actualPort());
                    serviceOptions.serviceDiscovery.ifPresent(subscriber::onNext);
                    subscriber.onCompleted();
                    return;
                }
                if (event.failed()) {
                    subscriber.onError(event.cause());
                }
            })).flatMap(discoverableService -> discoverableService.closeDiscovery(record.get()))
                .map(__ -> Void.TYPE.cast(null));
    }

    private void setupRoutes() {
        httpServer.websocketHandler(new WebSocketCommandHandler(new CommandHandler(commands)));
        router.route(root() + "hystrix.stream")
                .handler(new SSEHandler(HystrixEventStreamHandler::handle));

        serviceOptions.serviceDiscovery.ifPresent(discovery ->
                router.route(root() + "service-discovery/:action")
                        .produces("application/json")
                        .handler(new ServiceDiscoveryHandler(discovery, record::get)));
        httpServer.requestHandler(router::accept);
    }

    private String serviceName() {
        return WebUtils.excludeEndDelimiter(WebUtils.excludeStartDelimiter(serviceOptions.serviceName));
    }

    private String root() {
        return WebUtils.includeEndDelimiter(WebUtils.includeStartDelimiter(serviceOptions.root));
    }
}
