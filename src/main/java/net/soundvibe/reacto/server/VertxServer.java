package net.soundvibe.reacto.server;

import io.vertx.core.http.HttpServer;
import io.vertx.core.json.*;
import io.vertx.core.logging.*;
import io.vertx.ext.web.Router;
import io.vertx.servicediscovery.*;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.discovery.ServiceDiscoveryLifecycle;
import net.soundvibe.reacto.server.handlers.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.*;
import rx.Observable;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static net.soundvibe.reacto.server.ServiceRecords.COMMANDS;
import static net.soundvibe.reacto.utils.WebUtils.*;

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
        this.metadataJson = createMetadata();
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
                .flatMap(rec -> discoveryLifecycle.isClosed() ?
                    discoveryLifecycle.startDiscovery(rec) :
                    Observable.just(rec))
                .doOnNext(this::startHeartBeat)
                .doOnNext(rec -> Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.info("Executing shutdown hook...");
                    if (discoveryLifecycle.isOpen()) {
                        discoveryLifecycle.closeDiscovery(rec).subscribe(
                                r -> log.debug("Service discovery closed successfully"),
                                e -> log.debug("Error when closing service discovery: " + e)
                        );
                    }
                })))
                .doOnNext(record::set))
                .map(r -> httpServer);
    }

    private void startHeartBeat(Record record) {
        Scheduler.scheduleAtFixedInterval(TimeUnit.MINUTES.toMillis(1L), () -> {
            if (discoveryLifecycle.isOpen()) {
                discoveryLifecycle.publish(record)
                        .subscribe(rec -> log.info("Heartbeat published record: " + rec),
                                throwable -> log.error("Error while trying to publish the record on heartbeat: " + throwable),
                                () -> log.info("Heartbeat completed successfully"));
            } else {
                log.info("Skipping heartbeat because service discovery is closed");
            }
        }, "service-discovery-heartbeat");
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
                .map(commandDescriptor -> new JsonObject()
                        .put(CommandDescriptor.COMMAND, commandDescriptor.commandType)
                        .put(CommandDescriptor.EVENT, commandDescriptor.eventType)
                )
                .reduce(new JsonArray(), JsonArray::add, JsonArray::addAll);
    }

    @Override
    public Observable<Void> stop() {
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
                .map(__ -> Void.TYPE.cast(null));
    }

    private void setupRoutes() {
        httpServer.websocketHandler(new WebSocketCommandHandler(new CommandHandler(commands), root()));
        router.route(root() + "hystrix.stream")
            .handler(new SSEHandler(HystrixEventStreamHandler::handle));

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
