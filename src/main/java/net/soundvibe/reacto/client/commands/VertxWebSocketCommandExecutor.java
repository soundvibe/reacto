package net.soundvibe.reacto.client.commands;

import net.soundvibe.reacto.client.errors.CannotConnectToWebSocket;
import net.soundvibe.reacto.client.events.EventHandlers;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author OZY on 2016.05.09.
 */
public class VertxWebSocketCommandExecutor implements CommandExecutor {

    private final Supplier<Optional<EventHandlers>> eventHandlers;

    public VertxWebSocketCommandExecutor(Supplier<Optional<EventHandlers>> eventHandlers) {
        this.eventHandlers = eventHandlers;
    }

    @Override
    public Observable<Event> execute(Command command) {
        return eventHandlers.get()
                .map(handlers -> handlers.fallbackNodeClient.isPresent() ?
                        handlers.mainNodeClient.toObservable(command).onExceptionResumeNext(handlers.fallbackNodeClient.get().toObservable(command)) :
                        handlers.mainNodeClient.toObservable(command))
                .orElseGet(() -> Observable.error(new CannotConnectToWebSocket("Unable to execute command: " + command)))
                ;
    }
}
