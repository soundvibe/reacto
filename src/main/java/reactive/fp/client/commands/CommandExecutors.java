package reactive.fp.client.commands;

import reactive.fp.client.commands.hystrix.HystrixCommandExecutor;
import reactive.fp.client.commands.hystrix.HystrixObservableCommandWrapper;
import reactive.fp.client.commands.hystrix.HystrixTimeOutCommandExecutor;
import reactive.fp.client.errors.CommandNotFound;
import reactive.fp.client.events.VertxWebSocketEventHandler;
import reactive.fp.mappers.Mappers;
import rx.Observable;

import java.util.function.Function;

/**
 * @author Cipolinas on 2015.12.01.
 */
public interface CommandExecutors {

    int DEFAULT_EXECUTION_TIMEOUT = 1000;

    static <T> CommandExecutor<T> webSocket(CommandDef commandDef) {
        return Mappers.<T>mapToEventHandlers(commandDef, uri -> new VertxWebSocketEventHandler<>(uri, commandDef.eventClass))
                .map(eventHandlers -> new HystrixCommandExecutor<>(commandDef.name, eventHandlers))
                .orElseThrow(() -> new CommandNotFound(commandDef.name));
    }

    static <T> CommandExecutor<T> webSocket(CommandDef commandDef, int executionTimeoutInMs) {
        return Mappers.<T>mapToEventHandlers(commandDef, uri -> new VertxWebSocketEventHandler<>(uri, commandDef.eventClass))
                .map(eventHandlers -> new HystrixTimeOutCommandExecutor<>(commandDef.name, eventHandlers, executionTimeoutInMs))
                .orElseThrow(() -> new CommandNotFound(commandDef.name));
    }

    static <T> CommandExecutor<T> inMemory(String commandName, Function<Object, Observable<T>> commandExecutor) {
        return arg -> new HystrixObservableCommandWrapper<>(commandName, commandExecutor, arg, 0).toObservable();
    }

    static <T> CommandExecutor<T> inMemory(String commandName, Function<Object, Observable<T>> commandExecutor, int executionTimeoutInMs) {
        return arg -> new HystrixObservableCommandWrapper<>(commandName, commandExecutor, arg, executionTimeoutInMs).toObservable();
    }

}
