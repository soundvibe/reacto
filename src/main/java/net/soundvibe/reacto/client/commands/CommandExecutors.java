package net.soundvibe.reacto.client.commands;

import com.netflix.hystrix.HystrixCommandProperties;
import io.vertx.servicediscovery.Record;
import net.soundvibe.reacto.client.commands.hystrix.*;
import net.soundvibe.reacto.client.errors.CannotDiscoverService;
import net.soundvibe.reacto.client.events.*;
import net.soundvibe.reacto.discovery.*;
import net.soundvibe.reacto.mappers.Mappers;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;

import java.util.Optional;
import java.util.function.*;

/**
 * @author Cipolinas on 2015.12.01.
 */
public interface CommandExecutors {

    static HystrixCommandProperties.Setter defaultHystrixSetter() {
        return HystrixCommandProperties.defaultSetter()
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
                .withRequestLogEnabled(true)
        ;
    }

    static Observable<CommandExecutor> find(Services services) {
        return find(services, LoadBalancers.ROUND_ROBIN, Factories.ALL_RECORDS);
    }

    static Observable<CommandExecutor> find(Services services, Predicate<Record> filter) {
        return find(services, LoadBalancers.ROUND_ROBIN, filter);
    }

    static Observable<CommandExecutor> find(Services services, LoadBalancer loadBalancer) {
        return find(services, loadBalancer, Factories.ALL_RECORDS);
    }

    static Observable<CommandExecutor> find(Services services, LoadBalancer loadBalancer, Predicate<Record> filter) {
        return DiscoverableServices.find(services.mainServiceName, filter, services.serviceDiscovery, loadBalancer)
                .map(webSocketStream -> Pair.of(true, webSocketStream))
                .onErrorResumeNext(throwable -> services.fallbackServiceName.isPresent() ?
                        DiscoverableServices.find(services.fallbackServiceName.get(), filter, services.serviceDiscovery, loadBalancer)
                            .map(webSocketStream -> Pair.of(false, webSocketStream)):
                        Observable.error(new CannotDiscoverService("Cannot find any of " + services, throwable))
                )
                .flatMap(pair -> Observable.just(pair.value)
                                .concatWith(pair.key && services.fallbackServiceName.isPresent() ?
                                        DiscoverableServices.find(services.fallbackServiceName.get(), filter, services.serviceDiscovery, loadBalancer)
                                            .onExceptionResumeNext(Observable.empty()):
                                        Observable.empty())
                )
                .switchIfEmpty(Observable.error(new CannotDiscoverService("Unable to discover any of " + services)))
                .map(webSocketStream -> new VertxDiscoverableEventHandler(webSocketStream, VertxWebSocketEventHandler::observe))
                .toList()
                .filter(vertxDiscoverableEventHandlers -> !vertxDiscoverableEventHandlers.isEmpty())
                .switchIfEmpty(Observable.error(new CannotDiscoverService("Unable to discover any of " + services)))
                .map(vertxDiscoverableEventHandlers -> new EventHandlers(vertxDiscoverableEventHandlers.get(0),
                        vertxDiscoverableEventHandlers.stream()
                                .skip(1L)
                                .findFirst()
                                .map(vertxDiscoverableEventHandler -> (EventHandler)vertxDiscoverableEventHandler))
                )
                .map(eventHandlers -> new VertxWebSocketCommandExecutor(() -> Optional.ofNullable(eventHandlers)))
                ;
    }

    static CommandExecutor webSocket(Nodes nodes) {
        return new VertxWebSocketCommandExecutor(Mappers.mapToEventHandlers(nodes, VertxWebSocketEventHandler::new));
    }

    static CommandExecutor webSocket(Nodes nodes, int executionTimeoutInMs) {
        return new HystrixCommandExecutor(Mappers.mapToEventHandlers(nodes, VertxWebSocketEventHandler::new),
                defaultHystrixSetter().withExecutionTimeoutInMilliseconds(executionTimeoutInMs).withExecutionTimeoutEnabled(true));
    }

    static CommandExecutor webSocket(Nodes nodes, HystrixCommandProperties.Setter hystrixConfig) {
        return new HystrixCommandExecutor(Mappers.mapToEventHandlers(nodes, VertxWebSocketEventHandler::new), hystrixConfig);
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> commandExecutor) {
        return commandExecutor::apply;
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> commandExecutor, int executionTimeoutInMs) {
        return cmd -> new HystrixObservableCommandWrapper(commandExecutor, cmd,
                defaultHystrixSetter().withExecutionTimeoutInMilliseconds(executionTimeoutInMs).withExecutionTimeoutEnabled(true))
                .toObservable();
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> commandExecutor, HystrixCommandProperties.Setter hystrixConfig) {
        return cmd -> new HystrixObservableCommandWrapper(commandExecutor, cmd, hystrixConfig).toObservable();
    }

    static CommandExecutor inMemoryWithFallback(Function<Command, Observable<Event>> mainExecutor, Function<Command, Observable<Event>> fallbackExecutor) {
        return cmd -> new HystrixObservableCommandWrapper(mainExecutor, fallbackExecutor, cmd, defaultHystrixSetter()).toObservable();
    }

    static CommandExecutor inMemoryWithFallback(Function<Command, Observable<Event>> mainExecutor, Function<Command, Observable<Event>> fallbackExecutor,
                                    int executionTimeoutInMs) {
        return cmd -> new HystrixObservableCommandWrapper(mainExecutor, fallbackExecutor, cmd,
                defaultHystrixSetter().withExecutionTimeoutInMilliseconds(executionTimeoutInMs).withExecutionTimeoutEnabled(true)).toObservable();
    }

    static CommandExecutor inMemoryWithFallback(Function<Command, Observable<Event>> mainExecutor, Function<Command, Observable<Event>> fallbackExecutor,
                                                HystrixCommandProperties.Setter hystrixConfig) {
        return cmd -> new HystrixObservableCommandWrapper(mainExecutor, fallbackExecutor, cmd, hystrixConfig).toObservable();
    }

}
