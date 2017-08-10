package net.soundvibe.reacto.client.commands.hystrix;

import com.netflix.hystrix.HystrixCommandProperties;
import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.events.CommandHandler;
import net.soundvibe.reacto.errors.*;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.*;

/**
 * @author OZY on 2015.11.13.
 */
public final class HystrixCommandExecutor implements CommandExecutor {

    private final List<CommandHandler> commandHandlers;
    private final HystrixCommandProperties.Setter hystrixConfig;

    public HystrixCommandExecutor(List<CommandHandler> commandHandlers, HystrixCommandProperties.Setter hystrixConfig) {
        Objects.requireNonNull(commandHandlers, "commandHandlers cannot be null");
        Objects.requireNonNull(hystrixConfig, "hystrixConfig cannot be null");
        this.commandHandlers = commandHandlers;
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

    public static final CommandExecutorFactory FACTORY = (eventHandlers, loadBalancer) ->
            new HystrixCommandExecutor(eventHandlers, defaultHystrixSetter);

    @Override
    public Observable<Event> execute(Command command) {
        if (commandHandlers.isEmpty()) return Observable.error(new CannotFindEventHandlers("No event handlers found for command: " + command));
        return Observable.just(commandHandlers)
                .concatMap(handlers -> handlers.size() < 2 ?
                        new HystrixObservableCommandWrapper(
                                cmd -> commandHandlers.get(0).observe(cmd),
                                command,
                                hystrixConfig).toObservable() :
                        new HystrixObservableCommandWrapper(
                                cmd -> commandHandlers.get(0).observe(cmd),
                                cmd -> commandHandlers.get(1).observe(cmd),
                                command,
                                hystrixConfig).toObservable()
                );
    }
}
