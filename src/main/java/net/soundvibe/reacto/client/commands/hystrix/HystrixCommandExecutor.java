package net.soundvibe.reacto.client.commands.hystrix;

import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.types.Event;
import net.soundvibe.reacto.client.errors.CannotConnectToWebSocket;
import net.soundvibe.reacto.client.events.EventHandlers;
import net.soundvibe.reacto.types.Command;
import rx.Observable;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixCommandExecutor implements CommandExecutor {

    private final Supplier<Optional<EventHandlers>> eventHandlers;

    public HystrixCommandExecutor(Supplier<Optional<EventHandlers>> eventHandlers) {
        this.eventHandlers = eventHandlers;
    }


    @Override
    public Observable<Event> execute(Command command) {
        return eventHandlers.get()
                .map(eventHandlers -> new HystrixDistributedObservableCommand(command, eventHandlers, false, 0)
                        .toObservable())
                .orElse(Observable.error(new CannotConnectToWebSocket("Cannot connect to ws of command: " + command)));
    }

    @Override
    public String toString() {
        return "HystrixCommandExecutor{" +
                "eventHandlers=" + eventHandlers +
                '}';
    }

}
