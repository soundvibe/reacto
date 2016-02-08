package reactive.fp.client.commands;

import reactive.fp.client.commands.hystrix.HystrixCommandExecutor;
import reactive.fp.client.commands.hystrix.HystrixObservableCommandWrapper;
import reactive.fp.client.commands.hystrix.HystrixTimeOutCommandExecutor;
import reactive.fp.client.events.VertxWebSocketEventHandler;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import rx.Observable;

import java.util.function.Function;

import static reactive.fp.mappers.Mappers.mapToEventHandlers;

/**
 * @author Cipolinas on 2015.12.01.
 */
public interface CommandExecutors {

    int DEFAULT_EXECUTION_TIMEOUT = 1000;

    static CommandExecutor webSocket(Nodes nodes) {
        return new HystrixCommandExecutor(mapToEventHandlers(nodes, VertxWebSocketEventHandler::new));
    }

    static CommandExecutor webSocket(Nodes nodes, int executionTimeoutInMs) {
        return new HystrixTimeOutCommandExecutor(mapToEventHandlers(nodes, VertxWebSocketEventHandler::new),
                executionTimeoutInMs);
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> commandExecutor) {
        return cmd -> new HystrixObservableCommandWrapper(commandExecutor, cmd, 0).toObservable();
    }

    static CommandExecutor inMemory(Function<Command, Observable<Event>> commandExecutor, int executionTimeoutInMs) {
        return cmd -> new HystrixObservableCommandWrapper(commandExecutor, cmd, executionTimeoutInMs).toObservable();
    }

}
