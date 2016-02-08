package reactive.fp.server;

import reactive.fp.types.*;
import rx.Observable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

/**
 * @author Linas on 2015.11.12.
 */
public final class CommandRegistry {

    private final Map<String, Function<Command, Observable<Event>>> commands = new ConcurrentHashMap<>();

    private CommandRegistry() {
        //
    }

    @SuppressWarnings("unchecked")
    public CommandRegistry and(String commandName, Function<Command, Observable<Event>> onInvoke) {
        Objects.requireNonNull(commandName, "Command name cannot be null");
        Objects.requireNonNull(onInvoke, "onInvoke cannot be null");
        commands.put(commandName, onInvoke.compose(o -> o));
        return this;
    }

    public static CommandRegistry of(String commandName, Function<Command, Observable<Event>> onInvoke) {
        return new CommandRegistry().and(commandName, onInvoke);
    }

    public Optional<Function<Command, Observable<Event>>> findCommand(String address) {
        return Optional.ofNullable(commands.get(address));
    }

    public void foreach(Consumer<String> consumer) {
        commands.keySet().forEach(consumer);
    }

    @Override
    public String toString() {
        return "CommandRegistry{" +
                "commands=" + commands +
                '}';
    }

}
