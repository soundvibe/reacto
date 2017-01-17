package net.soundvibe.reacto.server.vertx;

import io.vertx.core.http.HttpServer;
import io.vertx.core.json.*;
import io.vertx.core.logging.*;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.discovery.ServiceDiscoveryLifecycle;
import net.soundvibe.reacto.server.*;
import net.soundvibe.reacto.server.vertx.handlers.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.WebUtils;
import rx.Observable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import static net.soundvibe.reacto.server.vertx.ServiceRecords.COMMANDS;
import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author OZY on 2015.11.23.
 */
public class VertxServer implements Server<HttpServer> {

    public static final int INTERNAL_SERVER_ERROR = 500;

    private static final Logger log = LoggerFactory.getLogger(VertxServer.class);

    public static final String HYSTRIX_STREAM_PATH = "hystrix.stream";
    public static final String REACTO_STREAM_PATH = "reacto.stream";
    public static final String ROUTES = "routes";
    public static final String VERSION = "version";

    private final ServiceOptions serviceOptions;
    private final CommandRegistry commands;
    private final HttpServer httpServer;
    private final Router router;
    private final AtomicReference<Record> record = new AtomicReference<>();
    private final ServiceDiscoveryLifecycle<Record> discoveryLifecycle;

    public VertxServer(
            ServiceOptions serviceOptions,
            Router router,
            HttpServer httpServer,
            CommandRegistry commands,
            ServiceDiscoveryLifecycle<Record> discoveryLifecycle) {
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
        }).flatMap(server -> Observable.just(createRecord(server.actualPort()))
                .flatMap(discoveryLifecycle::startDiscovery)
                .doOnNext(record::set))
                .map(r -> httpServer);
    }

    private Record createRecord(int port) {
        final String host = WebUtils.getLocalAddress();
        return HttpEndpoint.createRecord(
                serviceName(),
                host,
                port,
                root(),
                createMetadata(host, port)
        );
    }

    private JsonObject createMetadata(String host, int port) {
        return new JsonObject()
                .put(VERSION, serviceOptions.version)
                .put(COMMANDS, commandsToJsonArray(commands))
                .put(ROUTES, routesToJsonArray(router, host, port, serviceOptions.isSsl));
    }

    static JsonArray commandsToJsonArray(CommandRegistry commands) {
        return commands.stream()
                .map(Pair::getKey)
                .map(commandDescriptor -> new JsonObject()
                        .put(CommandDescriptor.COMMAND, commandDescriptor.commandType)
                        .put(CommandDescriptor.EVENT, commandDescriptor.eventType)
                )
                .reduce(new JsonArray(), JsonArray::add, JsonArray::addAll);
    }

    static JsonArray routesToJsonArray(Router router, String host, int port, boolean isSsl) {
        final String endpoint = isSsl ? "https://" : "http://" + host + ":" + port;
        return router.getRoutes().stream()
                .map(route -> includeEndDelimiter(endpoint) + excludeStartDelimiter(route.getPath()))
                .reduce(new JsonArray(), JsonArray::add, JsonArray::addAll);
    }

    @Override
    public Observable<Any> stop() {
        return Observable.<Record>create(subscriber ->
            httpServer.close(event -> {
                if (event.succeeded()) {
                    log.info("Server has stopped on port " + httpServer.actualPort());
                    if (!subscriber.isUnsubscribed()) {
                        final Record record = this.record.get();
                        if (record != null) {
                            subscriber.onNext(record);
                        }
                        subscriber.onCompleted();
                    }
                    return;
                }
                if (event.failed() && !subscriber.isUnsubscribed()) {
                    subscriber.onError(event.cause());
                }
            })).flatMap(rec -> discoveryLifecycle.isOpen() ?
                    discoveryLifecycle.closeDiscovery(rec) :
                    Observable.just(rec))
                .map(__ -> Any.VOID);
    }

    private void setupRoutes() {
        httpServer.websocketHandler(new WebSocketCommandHandler(new CommandProcessor(commands), root()));
        router.route(root() + HYSTRIX_STREAM_PATH)
            .handler(new SSEHandler(HystrixEventStreamHandler::handle));

        router.route(root() + REACTO_STREAM_PATH)
                .handler(new SSEHandler(new ReactoCommandMetricsStreamHandler()));

        router.route(root() + "service-discovery/:action")
            .produces("application/json")
            .handler(new ServiceDiscoveryHandler(discoveryLifecycle, record::get));
        httpServer.requestHandler(router::accept);
    }

    private String serviceName() {
        return excludeEndDelimiter(excludeStartDelimiter(serviceOptions.serviceName));
    }

    private String root() {
        return includeEndDelimiter(includeStartDelimiter(serviceOptions.root));
    }

}
