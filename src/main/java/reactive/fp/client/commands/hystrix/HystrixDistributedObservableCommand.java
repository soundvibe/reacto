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
class HystrixDistributedObservableCommand<T, U> extends HystrixObservableCommand<Event<U>> {

    private final T arg;
    private final String commandName;
    private final EventHandlers<T,U> eventHandlers;

    public HystrixDistributedObservableCommand(final T arg, String commandName, EventHandlers<T,U> eventHandlers,
                                               boolean useExecutionTimeout, int executionTimeoutInMs) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("group: " + commandName))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withFallbackEnabled(eventHandlers.fallbackNodeClient.isPresent())
                        .withExecutionTimeoutEnabled(useExecutionTimeout)
                        .withExecutionTimeoutInMilliseconds(executionTimeoutInMs)
                )
                .andCommandKey(HystrixCommandKey.Factory.asKey(resolveCommandName(commandName, useExecutionTimeout))));
        this.arg = arg;
        this.commandName = commandName;
        this.eventHandlers = eventHandlers;
    }

    protected static String resolveCommandName(String name, boolean useExecutionTimeout) {
        return useExecutionTimeout ? name : name + "$";
    }

    @Override
    protected Observable<Event<U>> construct() {
        return eventHandlers.mainNodeClient.toObservable(commandName, arg);
    }

    @Override
    protected Observable<Event<U>> resumeWithFallback() {
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
