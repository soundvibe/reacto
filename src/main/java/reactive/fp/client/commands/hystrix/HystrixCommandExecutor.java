package reactive.fp.client.commands.hystrix;

import reactive.fp.client.commands.CommandExecutor;
import reactive.fp.client.events.EventHandlers;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixCommandExecutor implements CommandExecutor {

    private final EventHandlers eventHandlers;

    public HystrixCommandExecutor(EventHandlers eventHandlers) {
        this.eventHandlers = eventHandlers;
    }

    @Override
    public Observable<Event> execute(Command command) {
        return new HystrixDistributedObservableCommand(command, eventHandlers, false, 0)
                .toObservable();
    }

    @Override
    public String toString() {
        return "HystrixCommandExecutor{" +
                "eventHandlers=" + eventHandlers +
                '}';
    }

}
