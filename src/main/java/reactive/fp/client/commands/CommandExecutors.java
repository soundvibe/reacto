package reactive.fp.client.commands;

import reactive.fp.client.commands.hystrix.HystrixCommandExecutor;
import reactive.fp.client.commands.hystrix.HystrixObservableCommandWrapper;
import reactive.fp.client.commands.hystrix.HystrixTimeOutCommandExecutor;
import reactive.fp.client.errors.CommandNotFound;
import reactive.fp.client.events.VertxWebSocketEventHandler;
import reactive.fp.mappers.Mappers;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import rx.Observable;

import java.util.function.Function;

/**
 * @author Cipolinas on 2015.12.01.
 */
public interface CommandExecutors {

    int DEFAULT_EXECUTION_TIMEOUT = 1000;

    static CommandExecutor webSocket(CommandDef commandDef) {
        return Mappers.mapToEventHandlers(commandDef, VertxWebSocketEventHandler::new)
                .map(HystrixCommandExecutor::new)
                .orElseThrow(() -> new CommandNotFound(commandDef.name));
    }

    static CommandExecutor webSocket(CommandDef commandDef, int executionTimeoutInMs) {
        return Mappers.mapToEventHandlers(commandDef, VertxWebSocketEventHandler::new)
                .map(eventHandlers -> new HystrixTimeOutCommandExecutor(eventHandlers, executionTimeoutInMs))
                .orElseThrow(() -> new CommandNotFound(commandDef.name));
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> commandExecutor) {
        return arg -> new HystrixObservableCommandWrapper(commandExecutor, arg, 0).toObservable();
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> commandExecutor, int executionTimeoutInMs) {
        return arg -> new HystrixObservableCommandWrapper(commandExecutor, arg, executionTimeoutInMs).toObservable();
    }

}
