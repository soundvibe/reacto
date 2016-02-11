package net.soundvibe.reacto.server;

import net.soundvibe.reacto.types.Command;
import net.soundvibe.reacto.types.Event;
import net.soundvibe.reacto.types.Pair;
import rx.Observable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.*;

/**
 * @author Linas on 2015.11.12.
 */
public final class CommandRegistry implements Iterable<Pair<String, Function<Command, Observable<Event>>>> {

    private final Map<String, Function<Command, Observable<Event>>> commands = new ConcurrentHashMap<>();

    private CommandRegistry() {
        //hide constructor
    }

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

    public Stream<Pair<String, Function<Command, Observable<Event>>>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<Pair<String, Function<Command, Observable<Event>>>> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    @Override
    public String toString() {
        return "CommandRegistry{" +
                "commands=" + commands +
                '}';
    }

    @Override
    public Iterator<Pair<String, Function<Command, Observable<Event>>>> iterator() {
        final Iterator<Map.Entry<String, Function<Command, Observable<Event>>>> entryIterator = commands.entrySet().iterator();
        return new Iterator<Pair<String, Function<Command, Observable<Event>>>>() {
            @Override
            public boolean hasNext() {
                return entryIterator.hasNext();
            }

            @Override
            public Pair<String, Function<Command, Observable<Event>>> next() {
                final Map.Entry<String, Function<Command, Observable<Event>>> entry = entryIterator.next();
                return Pair.of(entry.getKey(), entry.getValue());
            }
        };
    }
}
