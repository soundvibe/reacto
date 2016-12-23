package net.soundvibe.reacto.client.commands;

import io.vertx.core.logging.*;
import net.soundvibe.reacto.client.errors.*;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.discovery.LoadBalancer;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.*;

/**
 * @author OZY on 2016.05.09.
 */
public class VertxWebSocketCommandExecutor implements CommandExecutor {

    private static final Logger log = LoggerFactory.getLogger(VertxWebSocketCommandExecutor.class);

    private final List<EventHandler> eventHandlers;
    private final LoadBalancer<EventHandler> loadBalancer;

    public VertxWebSocketCommandExecutor(List<EventHandler> eventHandlers, LoadBalancer<EventHandler> loadBalancer) {
        Objects.requireNonNull(eventHandlers, "eventHandlers cannot be null");
        Objects.requireNonNull(loadBalancer, "loadBalancer cannot be null");
        this.eventHandlers = eventHandlers;
        this.loadBalancer = loadBalancer;
    }

    @Override
    public Observable<Event> execute(Command command) {
        if (eventHandlers.isEmpty()) return Observable.error(new CannotDiscoverService("No event handlers found for command: " + command));
        return Observable.just(eventHandlers)
                .map(loadBalancer::balance)
                .flatMap(eventHandler -> eventHandler.toObservable(command)
                        .onBackpressureBuffer()
                        .onErrorResumeNext(error -> handleError(error, command, eventHandler)));

        /*return eventHandlers.get()
                .map(handlers -> handlers.fallbackNodeClient.isPresent() ?
                        handlers.mainNodeClient.toObservable(command)
                                .onBackpressureBuffer()
                                .onExceptionResumeNext(handlers.fallbackNodeClient.get()
                                        .toObservable(command)
                                        .onBackpressureBuffer()
                                ) :
                        handlers.mainNodeClient
                                .toObservable(command)
                                .onBackpressureBuffer()
                )
                .orElseGet(() -> Observable.error(new CannotConnectToWebSocket("Unable to execute command: " + command)))
                ;*/
    }

    private Observable<Event> handleError(Throwable error, Command command, EventHandler eventHandler) {
        eventHandlers.remove(eventHandler);
        if (eventHandlers.isEmpty()) {
            return Observable.error(error);
        }
        log.error("Handling error: " + error);
        return execute(command);
    }

}
