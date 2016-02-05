package reactive.fp.client.commands.hystrix;

import reactive.fp.client.commands.CommandExecutor;
import reactive.fp.client.events.EventHandlers;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixTimeOutCommandExecutor implements CommandExecutor {

    private final EventHandlers eventHandlers;
    private final int executionTimeOutInMs;

    public HystrixTimeOutCommandExecutor(EventHandlers eventHandlers, int executionTimeOutInMs) {
        this.eventHandlers = eventHandlers;
        this.executionTimeOutInMs = executionTimeOutInMs;
    }

    @Override
    public Observable<Event> execute(Command command) {
        return new HystrixDistributedObservableCommand(command, eventHandlers, true, executionTimeOutInMs)
                .toObservable();
    }

    @Override
    public String toString() {
        return "HystrixTimeOutCommandExecutor{" +
                "eventHandlers=" + eventHandlers +
                ", executionTimeOutInMs=" + executionTimeOutInMs +
                '}';
    }

}
