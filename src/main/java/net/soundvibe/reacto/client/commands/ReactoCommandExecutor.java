package net.soundvibe.reacto.client.commands;

import io.vertx.core.logging.*;
import net.soundvibe.reacto.client.errors.CannotFindEventHandlers;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.discovery.*;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.*;

/**
 * @author OZY on 2016.09.06.
 */
public final class ReactoCommandExecutor implements CommandExecutor {

    private static final Logger log = LoggerFactory.getLogger(ReactoCommandExecutor.class);

    private final List<EventHandler> eventHandlers;
    private final LoadBalancer<EventHandler> loadBalancer;
    private final ServiceRegistry serviceRegistry;

    public static final CommandExecutorFactory FACTORY = ReactoCommandExecutor::new;

    public ReactoCommandExecutor(List<EventHandler> eventHandlers,
                                 LoadBalancer<EventHandler> loadBalancer,
                                 ServiceRegistry serviceRegistry) {
        Objects.requireNonNull(eventHandlers, "eventHandlers cannot be null");
        Objects.requireNonNull(loadBalancer, "loadBalancer cannot be null");
        Objects.requireNonNull(serviceRegistry, "serviceRegistry cannot be null");
        this.eventHandlers = eventHandlers;
        this.loadBalancer = loadBalancer;
        this.serviceRegistry = serviceRegistry;
    }

    @Override
    public Observable<Event> execute(Command command) {
        if (eventHandlers.isEmpty()) return Observable.error(new CannotFindEventHandlers("No event handlers found for command: " + command));
        return Observable.just(eventHandlers)
                .map(loadBalancer::balance)
                .concatMap(eventHandler -> eventHandler.observe(command)
                        .onBackpressureBuffer()
                        .onErrorResumeNext(error -> handleError(error, command, eventHandler)))
                ;
    }

    private Observable<Event> handleError(Throwable error, Command command, EventHandler eventHandler) {
        return serviceRegistry.unpublish(eventHandler.serviceRecord())
                .doOnNext(any -> log.info("Unpublished record " + eventHandler.serviceRecord() + " because of error: " + error))
                .doOnNext(any -> removeHandler(eventHandler))
                .flatMap(any -> eventHandlers.isEmpty() ?  Observable.error(error) : Observable.just(command))
                .doOnNext(cmd -> log.error("Handling error: " + error))
                .flatMap(this::execute);
    }

    private synchronized void removeHandler(EventHandler eventHandler) {
        eventHandlers.remove(eventHandler);
    }
}
