package net.soundvibe.reacto.client.commands.hystrix;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import net.soundvibe.reacto.types.Event;
import net.soundvibe.reacto.types.Command;
import net.soundvibe.reacto.client.events.EventHandlers;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
class HystrixDistributedObservableCommand extends HystrixObservableCommand<Event> {

    private final Command command;
    private final EventHandlers eventHandlers;

    public HystrixDistributedObservableCommand(Command command, EventHandlers eventHandlers,
                                               boolean useExecutionTimeout, int executionTimeoutInMs) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("group: " + command.name))
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withFallbackEnabled(eventHandlers.fallbackNodeClient.isPresent())
                        .withExecutionTimeoutEnabled(useExecutionTimeout)
                        .withExecutionTimeoutInMilliseconds(executionTimeoutInMs)
                )
                .andCommandKey(HystrixCommandKey.Factory.asKey(resolveCommandName(command.name, useExecutionTimeout))));
        this.command = command;
        this.eventHandlers = eventHandlers;
    }

    protected static String resolveCommandName(String name, boolean useExecutionTimeout) {
        return useExecutionTimeout ? name : name + "$";
    }

    @Override
    protected Observable<Event> construct() {
        return eventHandlers.mainNodeClient.toObservable(command);
    }

    @Override
    protected Observable<Event> resumeWithFallback() {
        return eventHandlers.fallbackNodeClient
                .map(socketClient -> socketClient.toObservable(command))
                .orElseGet(() -> super.resumeWithFallback());
    }
}
