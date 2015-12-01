package reactive.fp.client.commands;

import reactive.fp.client.commands.hystrix.HystrixCommandExecutor;
import reactive.fp.client.errors.CommandNotFound;
import reactive.fp.client.events.VertxWebSocketEventHandler;
import reactive.fp.mappers.Mappers;

/**
 * @author Cipolinas on 2015.12.01.
 */
public interface CommandExecutors {

    static <T> CommandExecutor<T> webSocket(CommandDef commandDef) {
        return Mappers.<T>mapToEventHandlers(commandDef, uri -> new VertxWebSocketEventHandler<>(uri, commandDef.eventClass))
                .map(eventHandlers -> new HystrixCommandExecutor<>(commandDef.name, eventHandlers))
                .orElseThrow(() -> new CommandNotFound(commandDef.name));
    }
}
