package reactive.fp.repositories;

import reactive.fp.commands.CommandExecutor;
import reactive.fp.commands.hystrix.HystrixCommandExecutor;
import reactive.fp.config.CommandRepositoryConfig;
import reactive.fp.errors.CommandNotFound;
import reactive.fp.types.EventHandler;
import reactive.fp.vertx.VertxWebSocketEventHandler;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import static reactive.fp.mappers.Mappers.mapToEventHandlers;

/**
 * @author OZY on 2015.11.13.
 */
public class CommandRepository {

    private final CommandRepositoryConfig config;

    public CommandRepository(CommandRepositoryConfig config) {
        this.config = config;
    }

    public <T> Optional<CommandExecutor<T>> findCommand(String name) {
        return findCommand(name, VertxWebSocketEventHandler::new);
    }

    public <T> Optional<CommandExecutor<T>> findCommand(String name, Function<URI, EventHandler<T>> eventHandlerFactory) {
        return config.findDistributedCommand(name)
                .flatMap(distributedCommandDef -> mapToEventHandlers(distributedCommandDef, eventHandlerFactory))
                .map(eventHandlers -> new HystrixCommandExecutor<>(name, eventHandlers));
    }

    public <T> CommandExecutor<T> getCommand(String name) {
        return getCommand(name, VertxWebSocketEventHandler::new);
    }

    public <T> CommandExecutor<T> getCommand(String name, Function<URI, EventHandler<T>> eventHandlerFactory) {
        return findCommand(name, eventHandlerFactory)
                .orElseThrow(() -> new CommandNotFound(name));
    }

    @Override
    public String toString() {
        return "CommandRepository{" +
                "config=" + config +
                '}';
    }
}
