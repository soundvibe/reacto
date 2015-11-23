package reactive.fp.client.commands.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import reactive.fp.types.Event;
import reactive.fp.client.events.EventHandlers;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixDistributedObservableCommand<T> extends HystrixObservableCommand<Event<?>> {

    private final T arg;
    private final String commandName;
    private final EventHandlers<T> eventHandlers;

    public HystrixDistributedObservableCommand(final T arg, String commandName, EventHandlers<T> eventHandlers) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("DistributedCommandsRegistry"))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withFallbackEnabled(true)
                        .withExecutionTimeoutInMilliseconds(5000)
                )
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandName)));
        this.arg = arg;
        this.commandName = commandName;
        this.eventHandlers = eventHandlers;
    }

    @Override
    protected Observable<Event<?>> construct() {
        return eventHandlers.mainNodeClient.toObservable(commandName, arg);
    }

    @Override
    protected Observable<Event<?>> resumeWithFallback() {
        return eventHandlers.fallbackNodeClient
                .map(socketClient -> socketClient.toObservable(commandName, arg))
                .orElseGet(() -> super.resumeWithFallback());
    }

    @Override
    public String toString() {
        return "HystrixDistributedObservableCommand{" +
                "arg=" + arg +
                ", commandName='" + commandName + '\'' +
                ", eventHandlers=" + eventHandlers +
                '}';
    }
}
