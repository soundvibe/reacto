package net.soundvibe.reacto.client.commands.hystrix;

import com.netflix.hystrix.HystrixCommandProperties;
import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.*;

/**
 * @author OZY on 2015.11.13.
 */
public class HystrixCommandExecutor implements CommandExecutor {

    private final List<EventHandler> eventHandlers;
    private final HystrixCommandProperties.Setter hystrixConfig;

    public HystrixCommandExecutor(List<EventHandler> eventHandlers, HystrixCommandProperties.Setter hystrixConfig) {
        Objects.requireNonNull(eventHandlers, "eventHandlers cannot be null");
        Objects.requireNonNull(hystrixConfig, "hystrixConfig cannot be null");
        this.eventHandlers = eventHandlers;
        this.hystrixConfig = hystrixConfig;
    }


    @Override
    public Observable<Event> execute(Command command) {
        if (eventHandlers.isEmpty()) return Observable.error(new CannotDiscoverService("No event handlers found for command: " + command));
        return Observable.just(eventHandlers)
                .flatMap(handlers -> handlers.size() < 1 ?
                        new HystrixObservableCommandWrapper(
                                cmd -> eventHandlers.get(0).observe(cmd),
                                command,
                                hystrixConfig).toObservable() :
                        new HystrixObservableCommandWrapper(
                                cmd -> eventHandlers.get(0).observe(cmd),
                                cmd -> eventHandlers.get(1).observe(cmd),
                                command,
                                hystrixConfig).toObservable()
                );
    }
}
