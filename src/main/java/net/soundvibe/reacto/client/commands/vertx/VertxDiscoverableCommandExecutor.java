package net.soundvibe.reacto.client.commands.vertx;

import io.vertx.core.logging.*;
import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.errors.CannotConnectToWebSocket;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.discovery.LoadBalancer;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.*;

/**
 * @author OZY on 2016.09.06.
 */
public final class VertxDiscoverableCommandExecutor implements CommandExecutor {

    private static final Logger log = LoggerFactory.getLogger(VertxDiscoverableCommandExecutor.class);

    private final List<EventHandler> eventHandlers;
    private final LoadBalancer<EventHandler> loadBalancer;

    public VertxDiscoverableCommandExecutor(List<EventHandler> eventHandlers,
                                            LoadBalancer<EventHandler> loadBalancer) {
        Objects.requireNonNull(eventHandlers, "eventHandlers cannot be null");
        Objects.requireNonNull(loadBalancer, "loadBalancer cannot be null");
        this.eventHandlers = eventHandlers;
        this.loadBalancer = loadBalancer;
    }

    @Override
    public Observable<Event> execute(Command command) {
        if (eventHandlers.isEmpty()) return Observable.error(new CannotConnectToWebSocket("No event handlers found for command: " + command));
        return Observable.just(eventHandlers)
                .map(loadBalancer::balance)
                .flatMap(eventHandler -> eventHandler.toObservable(command)
                        .onBackpressureBuffer()
                        .onErrorResumeNext(error -> handleError(error, command, eventHandler)))
                ;
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
