package reactive.fp.commands;

import rx.Observable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

/**
 * @author Linas on 2015.11.12.
 */
public class CommandRegistry {

    public static final Map<String, Function<Object, Observable<?>>> commands = new ConcurrentHashMap<>();

    public CommandRegistry register(String commandName, Function<Object, Observable<?>> onInvoke) {
        commands.put(commandName, onInvoke);
        return this;
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
