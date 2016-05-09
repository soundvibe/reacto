package net.soundvibe.reacto.client.commands.hystrix;

import com.netflix.hystrix.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.client.events.EventHandlers;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
class HystrixDistributedObservableCommand extends HystrixObservableCommand<Event> {

    private final Command command;
    private final EventHandlers eventHandlers;

    public HystrixDistributedObservableCommand(Command command, EventHandlers eventHandlers, HystrixCommandProperties.Setter setter) {
        super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("reacto"))
                .andCommandPropertiesDefaults(setter)
                .andCommandKey(HystrixCommandKey.Factory.asKey(resolveCommandName(command.name, setter.getExecutionTimeoutEnabled()))));
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
