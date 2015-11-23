package reactive.fp.commands.hystrix;

import reactive.fp.commands.CommandExecutor;
import reactive.fp.types.Event;
import reactive.fp.types.EventHandlers;
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
    public Observable<Event<?>> execute(T arg) {
        return new HystrixDistributedObservableCommand<>(arg, commandName, eventHandlers).toObservable();
    }

    @Override
    public String toString() {
        return "HystrixCommandExecutor{" +
                "commandName='" + commandName + '\'' +
                ", eventHandlers=" + eventHandlers +
                '}';
    }

}
