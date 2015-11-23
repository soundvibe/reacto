package reactive.fp.repositories;

import reactive.fp.commands.CommandExecutor;
import reactive.fp.commands.hystrix.HystrixCommandExecutor;
import reactive.fp.config.CommandRepositoryConfig;
import reactive.fp.types.*;
import reactive.fp.vertx.VertxWebSocketEventHandler;
import rx.Observable;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author OZY on 2015.11.13.
 */
public class CommandRepository {

    private final CommandRepositoryConfig config;

    public CommandRepository(CommandRepositoryConfig config) {
        this.config = config;
    }

    public <T,U> Optional<CommandExecutor<T,U>> findCommand(String name) {
        return findCommand(name, VertxWebSocketEventHandler::new);
    }

    public <T,U> Optional<CommandExecutor<T,U>> findCommand(String name, Function<URI, EventHandler<T,U>> eventHandlerFactory) {
        return config.findDistributedCommand(name)
                .<EventHandlers<T,U>>flatMap(distributedCommandDef -> mapToEventHandlers(distributedCommandDef, eventHandlerFactory))
                .map(eventHandlers -> new HystrixCommandExecutor<>(name, eventHandlers));
    }

    public <T,U> Observable<U> executeCommand(String name, T arg, Class<U> observableClass) {
        return findCommand(name)
                .map(executor -> executor.execute(arg))
                .map(o -> o.cast(observableClass))
                .orElse(Observable.error(new RuntimeException("Command " + name + " cannot be executed with arg " + arg)));
    }

    protected <T,U> Optional<EventHandlers<T,U>> mapToEventHandlers(DistributedCommandDef distributedCommandDef, Function<URI, EventHandler<T,U>> eventHandlerFactory) {
        return Optional.ofNullable(distributedCommandDef.mainURI())
                .map(eventHandlerFactory::apply)
                .map(mainEventHandler -> new EventHandlers<>(mainEventHandler, Optional.empty()))
                .map(eventHandlers -> distributedCommandDef.fallbackURI()
                        .map(eventHandlerFactory::apply)
                        .map(eventHandlers::copy)
                        .orElse(eventHandlers));
    }

    @Override
    public String toString() {
        return "CommandRepository{" +
                "config=" + config +
                '}';
    }
}
