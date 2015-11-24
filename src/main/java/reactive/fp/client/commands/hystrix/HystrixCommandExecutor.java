package reactive.fp.client.commands.hystrix;

import reactive.fp.client.commands.CommandExecutor;
import reactive.fp.types.Event;
import reactive.fp.client.events.EventHandlers;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixCommandExecutor<T> implements CommandExecutor<T> {

    public static int DEFAULT_EXECUTION_TIMEOUT = 1000;

    private final String commandName;
    private final EventHandlers<T> eventHandlers;

    public HystrixCommandExecutor(String commandName, EventHandlers<T> eventHandlers) {
        this.commandName = commandName;
        this.eventHandlers = eventHandlers;
    }

    @Override
    public Observable<Event<?>> execute(T arg) {
        return new HystrixDistributedObservableCommand<>(arg, commandName, eventHandlers, false, DEFAULT_EXECUTION_TIMEOUT)
                .toObservable();
    }

    @Override
    public Observable<Event<?>> observe(T arg) {
        return new HystrixDistributedObservableCommand<>(arg, commandName, eventHandlers, true, DEFAULT_EXECUTION_TIMEOUT)
                .toObservable();
    }

    @Override
    public String toString() {
        return "HystrixCommandExecutor{" +
                "commandName='" + commandName + '\'' +
                ", eventHandlers=" + eventHandlers +
                '}';
    }

}
