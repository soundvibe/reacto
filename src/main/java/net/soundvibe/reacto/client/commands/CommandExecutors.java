package net.soundvibe.reacto.client.commands;

import net.soundvibe.reacto.client.commands.hystrix.HystrixCommandExecutor;
import net.soundvibe.reacto.client.commands.hystrix.HystrixObservableCommandWrapper;
import net.soundvibe.reacto.client.commands.hystrix.HystrixTimeOutCommandExecutor;
import net.soundvibe.reacto.types.Event;
import net.soundvibe.reacto.client.events.VertxWebSocketEventHandler;
import net.soundvibe.reacto.types.Command;
import net.soundvibe.reacto.mappers.Mappers;
import rx.Observable;

import java.util.function.Function;

/**
 * @author Cipolinas on 2015.12.01.
 */
public interface CommandExecutors {

    int DEFAULT_EXECUTION_TIMEOUT = 1000;

    static CommandExecutor webSocket(Nodes nodes) {
        return new HystrixCommandExecutor(Mappers.mapToEventHandlers(nodes, VertxWebSocketEventHandler::new));
    }

    static CommandExecutor webSocket(Nodes nodes, int executionTimeoutInMs) {
        return new HystrixTimeOutCommandExecutor(Mappers.mapToEventHandlers(nodes, VertxWebSocketEventHandler::new),
                executionTimeoutInMs);
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> commandExecutor) {
        return cmd -> new HystrixObservableCommandWrapper(commandExecutor, cmd, 0).toObservable();
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> mainExecutor, Function<Command, Observable<Event>> fallbackExecutor) {
        return cmd -> new HystrixObservableCommandWrapper(mainExecutor, fallbackExecutor, cmd, 0).toObservable();
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> commandExecutor, int executionTimeoutInMs) {
        return cmd -> new HystrixObservableCommandWrapper(commandExecutor, cmd, executionTimeoutInMs).toObservable();
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> mainExecutor, Function<Command, Observable<Event>> fallbackExecutor,
                                    int executionTimeoutInMs) {
        return cmd -> new HystrixObservableCommandWrapper(mainExecutor, fallbackExecutor, cmd, executionTimeoutInMs).toObservable();
    }

}
