package net.soundvibe.reacto.discovery;

import io.vertx.core.logging.*;
import io.vertx.servicediscovery.*;
import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.events.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.server.ServiceRecords;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;

import java.time.Instant;
import java.util.List;
import java.util.function.*;

import static java.util.Comparator.comparing;

/**
 * @author OZY on 2016.08.26.
 */
public final class DiscoverableServices {

    private static final Logger log = LoggerFactory.getLogger(DiscoverableServices.class);

    /**
     * Finds all available services using service discovery
     * @param service The service to look for
     * @return Record observable, which emits multiple Records if services are found successfully
     */
    public static Observable<CommandExecutor> find(Service service) {
        return find(service.name, service.serviceDiscovery);
    }

    /**
     * Finds all available services using service discovery
     * @param serviceName The name of the service to look for
     * @param serviceDiscovery service discovery to use when looking for a service
     * @return Record observable, which emits multiple Records if services are found successfully
     */
    public static Observable<CommandExecutor> find(String serviceName, ServiceDiscovery serviceDiscovery) {
        return find(serviceName, Factories.ALL_RECORDS, serviceDiscovery, LoadBalancers.ROUND_ROBIN);
    }

    /**
     * Finds running service using service discovery and client load balancer
     * @param serviceName The name of the service to look for
     * @param filter additional predicate to filter found services
     * @param serviceDiscovery service discovery to use when looking for a service
     * @return Record observable, which emits multiple Records if services are found successfully
     */
    public static Observable<CommandExecutor> find( String serviceName,
                                                    Predicate<Record> filter,
                                                    ServiceDiscovery serviceDiscovery,
                                                    LoadBalancer<EventHandler> loadBalancer) {
        return findRecord(filter.and(record -> ServiceRecords.isService(serviceName, record)), serviceDiscovery, serviceName)
                .compose(records -> findExecutor(records, serviceName, serviceDiscovery, loadBalancer));
    }

    public static Observable<Event> execute(Command command, ServiceDiscovery serviceDiscovery) {
        return execute(command, serviceDiscovery, LoadBalancers.ROUND_ROBIN);
    }

    public static Observable<Event> execute(Command command, ServiceDiscovery serviceDiscovery, LoadBalancer<EventHandler> loadBalancer) {
        return findCommand(command.name, serviceDiscovery, loadBalancer)
                .flatMap(commandExecutor -> commandExecutor.execute(command));
    }

    private static Observable<CommandExecutor> findExecutor(Observable<Record> records,
                                                           String name,
                                                           ServiceDiscovery serviceDiscovery,
                                                           LoadBalancer<EventHandler> loadBalancer) {
        return records
                .switchIfEmpty(Observable.defer(() -> Observable.error(new CannotDiscoverService("Unable to discover any of " + name))))
                .map(record -> (EventHandler) new VertxDiscoverableEventHandler(record, serviceDiscovery, VertxWebSocketEventHandler::observe))
                .toList()
                .filter(vertxDiscoverableEventHandlers -> !vertxDiscoverableEventHandlers.isEmpty())
                .switchIfEmpty(Observable.defer(() -> Observable.error(new CannotDiscoverService("Unable to discover any of " + name))))
                .map(eventHandlers -> new VertxDiscoverableCommandExecutor(eventHandlers, loadBalancer));
    }

    public static Observable<CommandExecutor> findCommand(String commandName,
                                                          ServiceDiscovery serviceDiscovery) {
        return findCommand(commandName, serviceDiscovery, LoadBalancers.ROUND_ROBIN);
    }

    public static Observable<CommandExecutor> findCommand(String commandName,
                                                          ServiceDiscovery serviceDiscovery,
                                                          LoadBalancer<EventHandler> loadBalancer) {
        return findCommandRecords(commandName, serviceDiscovery)
                .compose(records -> findExecutor(records, commandName, serviceDiscovery, loadBalancer));
    }

    /**
     * Finds running services using service discovery and client load balancer
     * @param filter additional predicate to filter found services
     * @param serviceDiscovery service discovery to use when looking for a service
     * @param name name of something we are looking for. Needed for constructing an error if not found
     * @return Record observable, which emits multiple Records if services are found successfully
     */
    private static Observable<Record> findRecord(Predicate<Record> filter, ServiceDiscovery serviceDiscovery, String name) {
        return Observable.create(subscriber ->
                serviceDiscovery.getRecords(record -> ServiceRecords.isUpdatedRecently(record) && filter.test(record),
                        false,
                        asyncClients -> {
                            if (asyncClients.succeeded() && !subscriber.isUnsubscribed()) {
                                final List<Record> records = asyncClients.result();
                                if (!records.isEmpty()) {
                                    final Instant now = Instant.now();
                                    records.sort(comparing(rec -> rec.getMetadata().getInstant(ServiceRecords.LAST_UPDATED, now)));
                                    records.forEach(subscriber::onNext);
                                }
                                subscriber.onCompleted();
                            }
                            if (asyncClients.failed() && !subscriber.isUnsubscribed()) {
                                subscriber.onError(new CannotDiscoverService("Unable to find: " + name, asyncClients.cause()));
                            }
                        })
        );
    }

    /**
     * Finds running commands using service discovery and client load balancer
     * @param commandName The name of the command to look for
     * @param serviceDiscovery service discovery to use when looking for a service
     * @return Record observable, which emits multiple Records if services are found successfully
     */
    private static Observable<Record> findCommandRecords(String commandName,
                                                         ServiceDiscovery serviceDiscovery) {
        return findRecord(record -> ServiceRecords.hasCommand(commandName, record), serviceDiscovery, commandName);
    }

    public static Observable<Record> publishRecord(Record record, ServiceDiscovery serviceDiscovery) {
        return Observable.just(record)
                .flatMap(rec -> removeIf(rec, (existingRecord, newRecord) -> ServiceRecords.isDown(existingRecord), serviceDiscovery))
                .map(rec -> {
                    rec.getMetadata().put(ServiceRecords.LAST_UPDATED, Instant.now());
                    return rec.setStatus(Status.UP);
                })
                .flatMap(rec -> Observable.create(subscriber -> {
                    if (rec.getRegistration() != null) {
                        serviceDiscovery.update(record, recordEvent -> {
                            if (recordEvent.succeeded()) {
                                log.info("Service has been updated successfully: " + recordEvent.result().toJson());
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onNext(recordEvent.result());
                                    subscriber.onCompleted();
                                }
                            }
                            if (recordEvent.failed()) {
                                log.error("Error when trying to updated the service: " + recordEvent.cause(), recordEvent.cause());
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onError(recordEvent.cause());
                                }
                            }
                        });
                    } else {
                        serviceDiscovery.publish(rec, recordEvent -> {
                            if (recordEvent.succeeded()) {
                                log.info("Service has been published successfully: " + recordEvent.result().toJson());
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onNext(recordEvent.result());
                                    subscriber.onCompleted();
                                }
                            }
                            if (recordEvent.failed()) {
                                log.error("Error when trying to publish the service: " + recordEvent.cause(), recordEvent.cause());
                                if (!subscriber.isUnsubscribed()) {
                                    subscriber.onError(recordEvent.cause());
                                }
                            }
                        });
                    }
                }));
    }

    public static Observable<Record> removeRecordsWithStatus(Status status, ServiceDiscovery serviceDiscovery) {
        return Observable.create(subscriber ->
                serviceDiscovery.getRecords(
                        record -> status.equals(record.getStatus()),
                        true,
                        event -> {
                            if (event.succeeded()) {
                                if (event.result().isEmpty() && !subscriber.isUnsubscribed()) {
                                    subscriber.onCompleted();
                                    return;
                                }
                                Observable.from(event.result())
                                        .flatMap(record -> Observable.<Record>create(subscriber1 ->
                                                serviceDiscovery.unpublish(record.getRegistration(), e -> {
                                                    if (e.failed() && (!subscriber1.isUnsubscribed())) {
                                                        subscriber1.onError(e.cause());
                                                        return;
                                                    }
                                                    if (e.succeeded() && (!subscriber1.isUnsubscribed())) {
                                                        subscriber1.onNext(record);
                                                        subscriber1.onCompleted();
                                                    }
                                                })
                                        ))
                                        .subscribe(subscriber);
                            }
                            if (event.failed()) {
                                log.info("No matching records: " + event.cause());
                                subscriber.onError(event.cause());
                            }
                        }));
    }

    public static Observable<Record> removeIf(Record newRecord,
                                              BiPredicate<Record, Record> filter,
                                              ServiceDiscovery serviceDiscovery) {
        return Observable.create(subscriber ->
                serviceDiscovery.getRecords(
                        existingRecord -> filter.test(existingRecord, newRecord),
                        true,
                        event -> {
                            if (event.succeeded()) {
                                if (event.result().isEmpty() && !subscriber.isUnsubscribed()) {
                                    subscriber.onNext(newRecord);
                                    subscriber.onCompleted();
                                    return;
                                }

                                Observable.from(event.result())
                                        .doOnNext(record -> serviceDiscovery.release(serviceDiscovery.getReference(record)))
                                        .flatMap(record -> Observable.<Record>create(s -> serviceDiscovery.unpublish(record.getRegistration(), deleteEvent -> {
                                            if (deleteEvent.failed() && (!s.isUnsubscribed())) {
                                                s.onError(deleteEvent.cause());
                                            }
                                            if (deleteEvent.succeeded() && (!s.isUnsubscribed())) {
                                                s.onNext(record);
                                                s.onCompleted();
                                            }
                                        })))
                                        .subscribe(record -> log.info("Record was unpublished: " + record),
                                                throwable -> {
                                                    log.error("Error while trying to unpublish the record: " + throwable);
                                                    subscriber.onNext(newRecord);
                                                    subscriber.onCompleted();
                                                },
                                                () -> {
                                                    subscriber.onNext(newRecord);
                                                    subscriber.onCompleted();
                                                });
                            }
                            if (event.failed()) {
                                log.info("No matching records: " + event.cause());
                                subscriber.onError(event.cause());
                            }
                        }));
    }
}
