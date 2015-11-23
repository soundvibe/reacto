package reactive.fp.repositories;

import reactive.fp.commands.CommandExecutor;
import reactive.fp.commands.hystrix.HystrixCommandExecutor;
import reactive.fp.config.CommandRepositoryConfig;
import reactive.fp.types.DistributedCommandDef;
import reactive.fp.types.EventHandler;
import reactive.fp.types.EventHandlers;
import reactive.fp.jetty.JettyWebSocketEventHandler;
import rx.Observable;

import java.util.Optional;

/**
 * @author OZY on 2015.11.13.
 */
public class CommandRepository {

    private final CommandRepositoryConfig config;

    public CommandRepository(CommandRepositoryConfig config) {
        this.config = config;
    }

    public <T,U> Optional<CommandExecutor<T,U>> findCommand(String name) {
        return config.findDistributedCommand(name)
                .<EventHandlers<T,U>>flatMap(this::mapToEventHandlers)
                .map(eventHandlers -> new HystrixCommandExecutor<>(name, eventHandlers));
    }

    public <T,U> Observable<U> executeCommand(String name, T arg, Class<U> observableClass) {
        return findCommand(name)
                .map(executor -> executor.execute(arg))
                .map(o -> o.cast(observableClass))
                .orElse(Observable.error(new RuntimeException("Command " + name + " cannot be executed with arg " + arg)));
    }

    protected <T,U> Optional<EventHandlers<T,U>> mapToEventHandlers(DistributedCommandDef distributedCommandDef) {
        return EventHandler.<T,U>canStartEventHandler(distributedCommandDef.mainURI(), JettyWebSocketEventHandler::new)
                .map(mainEventHandler -> new EventHandlers<>(mainEventHandler, Optional.empty()))
                .map(eventHandlers -> distributedCommandDef.fallbackURI()
                        .flatMap(uri -> EventHandler.<T,U>canStartEventHandler(uri, JettyWebSocketEventHandler::new))
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
