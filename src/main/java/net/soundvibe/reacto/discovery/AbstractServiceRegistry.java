package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.errors.*;
import net.soundvibe.reacto.client.events.*;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.mappers.ServiceRegistryMapper;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @author Linas on 2017.01.18.
 */
public abstract class AbstractServiceRegistry implements ServiceRegistry {

    private final ServiceRegistryMapper mapper;
    private final EventHandlerRegistry eventHandlerRegistry;

    protected AbstractServiceRegistry(EventHandlerRegistry eventHandlerRegistry, ServiceRegistryMapper mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");
        Objects.requireNonNull(eventHandlerRegistry, "eventHandlerRegistry cannot be null");
        this.eventHandlerRegistry = eventHandlerRegistry;
        this.mapper = mapper;
    }

    protected Observable<Event> execute(Command command, LoadBalancer<EventHandler> loadBalancer,
                                        CommandExecutorFactory commandExecutorFactory) {
        return findRecordsOf(command)
                .compose(records -> findExecutor(records, command.name, loadBalancer, commandExecutorFactory))
                .concatMap(commandExecutor -> commandExecutor.execute(command));
    }

    /**
     * Finds up and running services using service discovery
     * @param command command for which to start looking for a service
     * @return Observable, which emits list of VertxRecords, if services are found successfully.
     * If no available services are found, CannotDiscoverService exception should be emitted.
     */
    protected abstract Observable<List<ServiceRecord>> findRecordsOf(Command command);

    Observable<CommandExecutor> findExecutor(Observable<List<ServiceRecord>> records,
                                                     String name,
                                                     LoadBalancer<EventHandler> loadBalancer,
                                                     CommandExecutorFactory commandExecutorFactory) {
        return records
                .filter(recs -> !recs.isEmpty())
                .switchIfEmpty(Observable.defer(() -> Observable.error(new CannotDiscoverService("Unable to discover any of " + name))))
                .flatMap(recs -> Observable.just(recs.stream()
                        .flatMap(eventHandlerRegistry::find)
                        .collect(toList()))
                        .flatMap(eventHandlers -> eventHandlers.isEmpty() ?
                                Observable.error(new CannotFindEventHandlers("Unable to find at least one compatible event handler for " + recs)) :
                                Observable.just(eventHandlers))
                )
                .map(eventHandlers -> commandExecutorFactory.create(eventHandlers, loadBalancer));
    }

    @Override
    public <E, C> Observable<E> execute(
            C command,
            Class<? extends E> eventClass,
            LoadBalancer<EventHandler> loadBalancer,
            CommandExecutorFactory commandExecutorFactory) {
        if (command == null) return Observable.error(new IllegalArgumentException("command cannot be null"));
        if (eventClass == null) return Observable.error(new IllegalArgumentException("eventClass cannot be null"));
        if (loadBalancer == null) return Observable.error(new IllegalArgumentException("loadBalancer cannot be null"));

        if (command instanceof Command && eventClass.isAssignableFrom(Event.class)) {
            //noinspection unchecked
            return (Observable<E>) execute((Command)command, loadBalancer, commandExecutorFactory);
        }

        return Observable.just(command)
                .map(cmd -> mapper.toCommand(cmd, eventClass))
                .concatMap(typedCommand -> execute(typedCommand, loadBalancer, commandExecutorFactory)).onBackpressureBuffer()
                .map(event -> mapper.toGenericEvent(event, eventClass));
    }
}
