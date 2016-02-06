package reactive.fp.client.commands.hystrix;

import reactive.fp.client.commands.CommandExecutor;
import reactive.fp.client.errors.CannotConnectToWebSocket;
import reactive.fp.client.events.EventHandlers;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import rx.Observable;

import java.util.Optional;
import java.util.function.Function;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixCommandExecutor implements CommandExecutor {

    private final Function<String, Optional<EventHandlers>> eventHandlers;

    public HystrixCommandExecutor(Function<String, Optional<EventHandlers>> eventHandlers) {
        this.eventHandlers = eventHandlers;
    }


    @Override
    public Observable<Event> execute(Command command) {
        return eventHandlers.apply(command.name)
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
