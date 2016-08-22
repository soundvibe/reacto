package net.soundvibe.reacto.server;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.server.handlers.SSEHandler;
import net.soundvibe.reacto.server.handlers.WebSocketCommandHandler;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import net.soundvibe.reacto.server.handlers.CommandHandler;
import net.soundvibe.reacto.server.handlers.HystrixEventStreamHandler;
import net.soundvibe.reacto.utils.WebUtils;
import rx.Observable;

import java.util.Objects;
import java.util.Optional;
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
                record = createRecord(event.result().actualPort());
                final Thread thread = new Thread(() -> publishRecord(countDownLatch, record, serviceDiscovery.get()));
                thread.setDaemon(true);
                thread.start();

                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    log.info("Executing shutdown hook...");
                    closeDiscovery(serviceDiscovery.get());
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

    private void publishRecord(CountDownLatch countDownLatch, Record record, ServiceDiscovery serviceDiscovery) {
        removeExistingServiceRecordsIfPresent(serviceDiscovery, record);
        serviceDiscovery.publish(
                record,
                recordEvent -> {
                    countDownLatch.countDown();
                    if (recordEvent.succeeded()) {
                        log.info("Service has been published successfully: " + recordEvent.result().toJson());
                    }
                    if (recordEvent.failed()) {
                        log.error("Error when trying to publish the service: " + recordEvent.cause(), recordEvent.cause());
                    }
                }
        );
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
                serviceDiscovery.ifPresent(this::closeDiscovery);
            }
        });
    }

    private void removeExistingServiceRecordsIfPresent(ServiceDiscovery serviceDiscovery, Record newRecord) {
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            serviceDiscovery.getRecords(
                    existingRecord -> sameRecord(existingRecord, newRecord),
                    true,
                    event -> {
                        if (event.succeeded()) {
                            Observable.from(event.result())
                                    .doOnNext(record -> serviceDiscovery.release(serviceDiscovery.getReference(record)))
                                    .flatMap(record -> Observable.create(subscriber -> {
                                                serviceDiscovery.unpublish(record.getRegistration(), e -> {
                                                    if (e.failed()) {
                                                        subscriber.onError(e.cause());
                                                        return;
                                                    }

                                                    if (e.succeeded()) {
                                                        subscriber.onNext(record);
                                                        subscriber.onCompleted();
                                                    }
                                                });
                                            }))
                                    .subscribe(record -> log.info("Record unpublished: " + record),
                                            throwable -> {
                                                log.error("Error when unpublishing record: " + throwable);
                                                countDownLatch.countDown();
                                            },
                                            countDownLatch::countDown);
                        }
                        if (event.failed()) {
                            log.info("No matching records: " + event.cause());
                            countDownLatch.countDown();
                        }
            });
            countDownLatch.await(1L, TimeUnit.MINUTES);
        } catch (Throwable e) {
            log.warn("Error when removing duplicates on service discovery: " + e);
        }
    }

    private boolean sameRecord(Record existingRecord, Record newRecord) {
        return  Objects.equals(newRecord.getName(), existingRecord.getName()) &&
                Objects.equals(newRecord.getLocation(), existingRecord.getLocation()) &&
                Objects.equals(newRecord.getType(), existingRecord.getType());
    }

    private void closeDiscovery(ServiceDiscovery serviceDiscovery) {
        try {
            log.info("Closing service discovery...");
            removeExistingServiceRecordsIfPresent(serviceDiscovery, record);
            serviceDiscovery.close();
        } catch (Throwable e) {
            log.warn("Error when closing service discovery: " + e);
        }
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
