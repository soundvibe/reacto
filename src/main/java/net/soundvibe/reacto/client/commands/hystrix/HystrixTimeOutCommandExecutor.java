package net.soundvibe.reacto.client.commands.hystrix;

import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.errors.CannotConnectToWebSocket;
import net.soundvibe.reacto.client.events.EventHandlers;
import net.soundvibe.reacto.types.Command;
import net.soundvibe.reacto.types.Event;
import rx.Observable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixTimeOutCommandExecutor implements CommandExecutor {

    private final Supplier<Optional<EventHandlers>> eventHandlers;
    private final int executionTimeOutInMs;

    public HystrixTimeOutCommandExecutor(Supplier<Optional<EventHandlers>> eventHandlers, int executionTimeOutInMs) {
        this.eventHandlers = eventHandlers;
        this.executionTimeOutInMs = executionTimeOutInMs;
    }

    @Override
    public Observable<Event> execute(Command command) {
        return eventHandlers.get()
                .map(eventHandlers -> new HystrixDistributedObservableCommand(command, eventHandlers, true, executionTimeOutInMs)
                        .toObservable())
                .orElse(Observable.error(new CannotConnectToWebSocket("Cannot connect to ws of command: " + command)));
    }
}
