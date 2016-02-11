package io.reacto.client.commands.hystrix;

import io.reacto.client.commands.CommandExecutor;
import io.reacto.client.errors.CannotConnectToWebSocket;
import io.reacto.client.events.EventHandlers;
import io.reacto.types.Command;
import io.reacto.types.Event;
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

    @Override
    public String toString() {
        return "HystrixTimeOutCommandExecutor{" +
                "eventHandlers=" + eventHandlers +
                ", executionTimeOutInMs=" + executionTimeOutInMs +
                '}';
    }

}
