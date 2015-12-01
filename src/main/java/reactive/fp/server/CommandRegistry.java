package reactive.fp.server;

import rx.Observable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

/**
 * @author Linas on 2015.11.12.
 */
public final class CommandRegistry {

    private final Map<String, Function<Object, Observable<?>>> commands = new ConcurrentHashMap<>();

    private CommandRegistry() {
        //
    }

    @SuppressWarnings("unchecked")
    public <T> CommandRegistry and(String commandName, Function<T, Observable<?>> onInvoke) {
        Objects.requireNonNull(commandName, "Command name cannot be null");
        Objects.requireNonNull(onInvoke, "onInvoke cannot be null");
        commands.put(commandName, onInvoke.compose(o -> (T) o));
        return this;
    }

    public static <T> CommandRegistry of(String commandName, Function<T, Observable<?>> onInvoke) {
        return new CommandRegistry().and(commandName, onInvoke);
    }

    public Optional<Function<Object, Observable<?>>> findCommand(String address) {
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
