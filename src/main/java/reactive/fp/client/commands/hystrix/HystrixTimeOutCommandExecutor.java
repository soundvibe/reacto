package reactive.fp.client.commands.hystrix;

import reactive.fp.client.commands.CommandExecutor;
import reactive.fp.client.events.EventHandlers;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixTimeOutCommandExecutor<T> implements CommandExecutor<T> {

    private final String commandName;
    private final EventHandlers<T> eventHandlers;
    private final int executionTimeOutInMs;

    public HystrixTimeOutCommandExecutor(String commandName, EventHandlers<T> eventHandlers, int executionTimeOutInMs) {
        this.commandName = commandName;
        this.eventHandlers = eventHandlers;
        this.executionTimeOutInMs = executionTimeOutInMs;
    }

    @Override
    public Observable<T> execute(Object arg) {
        return new HystrixDistributedObservableCommand<>(arg, commandName, eventHandlers, true, executionTimeOutInMs)
                .toObservable()
                .map(event -> event.payload)
                ;
    }

    @Override
    public String toString() {
        return "HystrixTimeOutCommandExecutor{" +
                "commandName='" + commandName + '\'' +
                ", eventHandlers=" + eventHandlers +
                ", executionTimeOutInMs=" + executionTimeOutInMs +
                '}';
    }

}
