package reactive.fp.client.commands.hystrix;

import reactive.fp.client.commands.CommandExecutor;
import reactive.fp.client.events.EventHandlers;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixCommandExecutor<T, U> implements CommandExecutor<T, U> {

    private final String commandName;
    private final EventHandlers<T,U> eventHandlers;

    public HystrixCommandExecutor(String commandName, EventHandlers<T,U> eventHandlers) {
        this.commandName = commandName;
        this.eventHandlers = eventHandlers;
    }

    @Override
    public Observable<U> execute(T arg) {
        return new HystrixDistributedObservableCommand<>(arg, commandName, eventHandlers, false, 0)
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
