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
class HystrixDistributedObservableCommand<T> extends HystrixObservableCommand<Event<T>> {

    private final Object arg;
    private final String commandName;
    private final EventHandlers<T> eventHandlers;

    public HystrixDistributedObservableCommand(final Object arg, String commandName, EventHandlers<T> eventHandlers,
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
    protected Observable<Event<T>> construct() {
        return eventHandlers.mainNodeClient.toObservable(commandName, arg);
    }

    @Override
    protected Observable<Event<T>> resumeWithFallback() {
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
