package net.soundvibe.reacto.discovery;

import io.reactivex.Flowable;
import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.events.*;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.errors.*;
import net.soundvibe.reacto.internal.*;
import net.soundvibe.reacto.mappers.ServiceRegistryMapper;
import net.soundvibe.reacto.metric.ObserverMetric;
import net.soundvibe.reacto.types.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

/**
 * @author Linas on 2017.01.18.
 */
public abstract class AbstractServiceRegistry implements ServiceRegistry {

    private final ServiceRegistryMapper mapper;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final Cache<String, Flowable<List<ServiceRecord>>> commandCache = ExpiringCache.periodically(10L, TimeUnit.SECONDS);

    protected AbstractServiceRegistry(CommandHandlerRegistry commandHandlerRegistry, ServiceRegistryMapper mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(commandHandlerRegistry, "commandHandlerRegistry cannot be null");
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.mapper = mapper;
    }

    protected Flowable<Event> execute(Command command, LoadBalancer<CommandHandler> loadBalancer,
                                      CommandExecutorFactory commandExecutorFactory) {
        return Flowable.fromCallable(() -> ObserverMetric.findObserver(command))
                .flatMap(metric -> Flowable.using(metric::startTimer,
                        pair -> commandCache.computeIfAbsent(commandKey(command), key -> findRecordsOf(command).cache())
                                .compose(records -> findExecutor(records, command.name, loadBalancer, commandExecutorFactory))
                                .concatMap(commandExecutor -> commandExecutor.execute(command))
                                .doOnEach(pair.key),
                        pair -> pair.value.stop())
                );
    }

    /**
     * Finds up and running services using service discovery
     * @param command command for which to start looking for a service
     * @return Observable, which emits list of VertxRecords, if services are found successfully.
     * If no available services are found, CannotDiscoverService exception should be emitted.
     */
    protected abstract Flowable<List<ServiceRecord>> findRecordsOf(Command command);

    private String commandKey(Command command) {
        return command.name + ":" + command.eventType();
    }

    Flowable<CommandExecutor> findExecutor(Flowable<List<ServiceRecord>> records,
                                                     String name,
                                                     LoadBalancer<CommandHandler> loadBalancer,
                                                     CommandExecutorFactory commandExecutorFactory) {
        return records
                .filter(recs -> !recs.isEmpty())
                .switchIfEmpty(Flowable.defer(() -> Flowable.error(new CannotDiscoverService("Unable to discover any of " + name))))
                .flatMap(recs -> Flowable.just(recs.stream()
                        .flatMap(commandHandlerRegistry::find)
                        .collect(toList()))
                        .flatMap(eventHandlers -> eventHandlers.isEmpty() ?
                                Flowable.error(new CannotFindEventHandlers("Unable to find at least one compatible event handler for " + recs)) :
                                Flowable.just(eventHandlers))
                )
                .map(eventHandlers -> commandExecutorFactory.create(eventHandlers, loadBalancer));
    }

    @Override
    public <E, C> Flowable<E> execute(
            C command,
            Class<? extends E> eventClass,
            LoadBalancer<CommandHandler> loadBalancer,
            CommandExecutorFactory commandExecutorFactory) {
        if (command == null) return Flowable.error(new IllegalArgumentException("command cannot be null"));
        if (eventClass == null) return Flowable.error(new IllegalArgumentException("eventClass cannot be null"));
        if (loadBalancer == null) return Flowable.error(new IllegalArgumentException("loadBalancer cannot be null"));

        if (command instanceof Command && eventClass.isAssignableFrom(Event.class)) {
            //noinspection unchecked
            return (Flowable<E>) execute((Command)command, loadBalancer, commandExecutorFactory);
        }

        return Flowable.just(command)
                .map(cmd -> mapper.toCommand(cmd, eventClass))
                .concatMap(typedCommand -> execute(typedCommand, loadBalancer, commandExecutorFactory))
                .map(event -> mapper.toGenericEvent(event, eventClass));
    }
}
