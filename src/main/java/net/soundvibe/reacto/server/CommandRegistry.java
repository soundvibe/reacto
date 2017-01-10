package net.soundvibe.reacto.server;

import net.soundvibe.reacto.mappers.CommandRegistryMapper;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.*;

/**
 * @author Linas on 2015.11.12.
 */
public final class CommandRegistry implements Iterable<Pair<CommandDescriptor, Function<Command, Observable<Event>>>> {

    private final Map<CommandDescriptor, Function<Command, Observable<Event>>> commands = new ConcurrentHashMap<>();
    private final CommandRegistryMapper mapper;

    private CommandRegistry() {
        this.mapper = null;
    }

    private CommandRegistry(CommandRegistryMapper mapper) {
        this.mapper = mapper;
    }

    public CommandRegistry and(String commandName, Function<Command, Observable<Event>> onInvoke) {
        Objects.requireNonNull(commandName, "Command name cannot be null");
        Objects.requireNonNull(onInvoke, "onInvoke cannot be null");
        commands.put(CommandDescriptor.of(commandName), onInvoke);
        return this;
    }

    public <C,E> CommandRegistry and(Class<C> commandType, Class<? extends E> eventType,
                                     Function<C, Observable<? extends E>> onInvoke) {
        Objects.requireNonNull(commandType, "commandType name cannot be null");
        Objects.requireNonNull(eventType, "eventType name cannot be null");
        Objects.requireNonNull(onInvoke, "onInvoke cannot be null");
        Objects.requireNonNull(mapper, "mapper cannot be null");

        final Function<Command, Observable<? extends E>> before = onInvoke
                .compose(c -> mapper.toGenericCommand(c, commandType));
        final Function<Command, Observable<Event>> after = before
                .andThen(observable -> observable.map(mapper::toEvent));
        commands.put(CommandDescriptor.ofTypes(commandType, eventType), after);
        return this;
    }

    public static CommandRegistry of(String commandName, Function<Command, Observable<Event>> onInvoke) {
        return new CommandRegistry().and(commandName, onInvoke);
    }

    public static <C,E> CommandRegistry ofTyped(
            Class<C> commandType, Class<? extends E> eventType,
            Function<C, Observable<? extends E>> onInvoke,
            CommandRegistryMapper mapper) {
        return new CommandRegistry(mapper).and(commandType, eventType, onInvoke);
    }

    public static CommandRegistry empty() {
        return new CommandRegistry();
    }

    public Optional<Function<Command, Observable<Event>>> findCommand(CommandDescriptor descriptor) {
        return Optional.ofNullable(commands.get(descriptor));
    }

    public Stream<Pair<CommandDescriptor, Function<Command, Observable<Event>>>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<Pair<CommandDescriptor, Function<Command, Observable<Event>>>> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    @Override
    public String toString() {
        return "CommandRegistry{" +
                "commands=" + commands +
                '}';
    }

    @Override
    public Iterator<Pair<CommandDescriptor, Function<Command, Observable<Event>>>> iterator() {
        final Iterator<Map.Entry<CommandDescriptor, Function<Command, Observable<Event>>>> entryIterator = commands.entrySet().iterator();
        return new Iterator<Pair<CommandDescriptor, Function<Command, Observable<Event>>>>() {
            @Override
            public boolean hasNext() {
                return entryIterator.hasNext();
            }

            @Override
            public Pair<CommandDescriptor, Function<Command, Observable<Event>>> next() {
                final Map.Entry<CommandDescriptor, Function<Command, Observable<Event>>> entry = entryIterator.next();
                return Pair.of(entry.getKey(), entry.getValue());
            }
        };
    }
}
