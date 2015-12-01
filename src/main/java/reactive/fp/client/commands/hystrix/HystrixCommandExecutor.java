package reactive.fp.client.commands.hystrix;

import reactive.fp.client.commands.CommandExecutor;
import reactive.fp.client.events.EventHandlers;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixCommandExecutor<T> implements CommandExecutor<T> {

    private final String commandName;
    private final EventHandlers<T> eventHandlers;

    public HystrixCommandExecutor(String commandName, EventHandlers<T> eventHandlers) {
        this.commandName = commandName;
        this.eventHandlers = eventHandlers;
    }

    @Override
    public Observable<T> execute(Object arg) {
        return new HystrixDistributedObservableCommand<>(arg, commandName, eventHandlers, false, DEFAULT_EXECUTION_TIMEOUT)
                .toObservable()
                .map(event -> event.payload)
                ;
    }

    @Override
    public Observable<T> observe(Object arg, int executionTimeoutInMs) {
        return new HystrixDistributedObservableCommand<>(arg, commandName, eventHandlers, true, executionTimeoutInMs)
                .toObservable()
                .map(event -> event.payload)
                ;
    }

    @Override
    public String toString() {
        return "HystrixCommandExecutor{" +
                "commandName='" + commandName + '\'' +
                ", eventHandlers=" + eventHandlers +
                '}';
    }

}
