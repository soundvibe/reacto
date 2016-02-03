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

    static <T, U> CommandExecutor<T, U> webSocket(CommandDef<U> commandDef) {
        return Mappers.<T,U>mapToEventHandlers(commandDef, uri -> new VertxWebSocketEventHandler<>(uri, commandDef.eventClass))
                .map(eventHandlers -> new HystrixCommandExecutor<>(commandDef.name, eventHandlers))
                .orElseThrow(() -> new CommandNotFound(commandDef.name));
    }

    static <T, U> CommandExecutor<T, U> webSocket(CommandDef<U> commandDef, int executionTimeoutInMs) {
        return Mappers.<T,U>mapToEventHandlers(commandDef, uri -> new VertxWebSocketEventHandler<>(uri, commandDef.eventClass))
                .map(eventHandlers -> new HystrixTimeOutCommandExecutor<>(commandDef.name, eventHandlers, executionTimeoutInMs))
                .orElseThrow(() -> new CommandNotFound(commandDef.name));
    }

    static <T, U> CommandExecutor<T, U> inMemory(String commandName, Function<T, Observable<U>> commandExecutor) {
        return arg -> new HystrixObservableCommandWrapper<>(commandName, commandExecutor, arg, 0).toObservable();
    }

    static <T, U> CommandExecutor<T, U> inMemory(String commandName, Function<T, Observable<U>> commandExecutor, int executionTimeoutInMs) {
        return arg -> new HystrixObservableCommandWrapper<>(commandName, commandExecutor, arg, executionTimeoutInMs).toObservable();
    }

}
