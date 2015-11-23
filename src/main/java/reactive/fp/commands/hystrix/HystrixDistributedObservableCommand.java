package reactive.fp.commands.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import reactive.fp.types.EventHandlers;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixDistributedObservableCommand<T, U> extends HystrixObservableCommand<U> {

    private final T arg;
    private final String commandName;
    private final EventHandlers<T,U> eventHandlers;

    public HystrixDistributedObservableCommand(final T arg, String commandName, EventHandlers<T,U> eventHandlers) {
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
    protected Observable<U> construct() {
        return eventHandlers.mainNodeClient.toObservable(commandName, arg);
    }

    @Override
    protected Observable<U> resumeWithFallback() {
        return eventHandlers.fallbackNodeClient
                .<Observable<U>>map(socketClient -> socketClient.toObservable(commandName, arg))
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
