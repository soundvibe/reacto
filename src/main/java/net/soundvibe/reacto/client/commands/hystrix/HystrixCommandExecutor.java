package net.soundvibe.reacto.client.commands.hystrix;

import com.netflix.hystrix.HystrixCommandProperties;
import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.errors.*;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.*;

/**
 * @author OZY on 2015.11.13.
 */
public final class HystrixCommandExecutor implements CommandExecutor {

    private final List<EventHandler> eventHandlers;
    private final HystrixCommandProperties.Setter hystrixConfig;

    public HystrixCommandExecutor(List<EventHandler> eventHandlers, HystrixCommandProperties.Setter hystrixConfig) {
        Objects.requireNonNull(eventHandlers, "eventHandlers cannot be null");
        Objects.requireNonNull(hystrixConfig, "hystrixConfig cannot be null");
        this.eventHandlers = eventHandlers;
        this.hystrixConfig = hystrixConfig;
    }

    public static final HystrixCommandProperties.Setter defaultHystrixSetter =
        HystrixCommandProperties.defaultSetter()
                .withExecutionIsolationThreadInterruptOnTimeout(false)
                .withExecutionTimeoutEnabled(false)
                .withExecutionTimeoutInMilliseconds(1000)
                .withExecutionIsolationSemaphoreMaxConcurrentRequests(10)
                .withFallbackIsolationSemaphoreMaxConcurrentRequests(10)
                .withFallbackEnabled(false)
                .withCircuitBreakerEnabled(true)
                .withCircuitBreakerRequestVolumeThreshold(20)
                .withCircuitBreakerSleepWindowInMilliseconds(5000)
                .withCircuitBreakerErrorThresholdPercentage(50)
                .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                .withRequestCacheEnabled(false)
                .withRequestLogEnabled(true);

    public static final CommandExecutorFactory FACTORY = (eventHandlers, loadBalancer, serviceRegistry) ->
            new HystrixCommandExecutor(eventHandlers, defaultHystrixSetter);

    @Override
    public Observable<Event> execute(Command command) {
        if (eventHandlers.isEmpty()) return Observable.error(new CannotFindEventHandlers("No event handlers found for command: " + command));
        return Observable.just(eventHandlers)
                .concatMap(handlers -> handlers.size() < 2 ?
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
