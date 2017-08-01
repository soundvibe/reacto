package net.soundvibe.reacto.server;

import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.mappers.CommandRegistryMapper;
import net.soundvibe.reacto.errors.CommandAlreadyRegistered;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.*;

import static java.util.Objects.requireNonNull;

/**
 * @author Linas on 2015.11.12.
 */
public final class CommandRegistry implements Iterable<Pair<CommandDescriptor, CommandExecutor>> {

    private final Map<CommandDescriptor, CommandExecutor> commands = new ConcurrentHashMap<>();
    private final CommandRegistryMapper mapper;

    private CommandRegistry() {
        this.mapper = null;
    }

    private CommandRegistry(CommandRegistryMapper mapper) {
        this.mapper = mapper;
    }

    public CommandRegistry and(String commandName, CommandExecutor onInvoke) {
        requireNonNull(commandName, "Command name cannot be null");
        requireNonNull(onInvoke, "onInvoke cannot be null");
        add(CommandDescriptor.of(commandName), onInvoke);
        return this;
    }

    public <C,E> CommandRegistry and(Class<C> commandType, Class<? extends E> eventType,
                                     Function<C, Observable<? extends E>> onInvoke) {
        requireNonNull(commandType, "commandType name cannot be null");
        requireNonNull(eventType, "eventType name cannot be null");
        requireNonNull(onInvoke, "onInvoke cannot be null");
        requireNonNull(mapper, "mapper cannot be null");

        final Function<Command, Observable<? extends E>> before = onInvoke
                .compose(c -> mapper.toGenericCommand(c, commandType));
        final Function<Command, Observable<Event>> after = before
                .andThen(observable -> observable.map(mapper::toEvent));
        add(CommandDescriptor.ofTypes(commandType, eventType), after::apply);
        return this;
    }

    private void add(CommandDescriptor descriptor, CommandExecutor onInvoke) {
        if (commands.containsKey(descriptor)) {
            throw new CommandAlreadyRegistered(descriptor);
        }
        commands.put(descriptor, onInvoke);
    }

    public static CommandRegistry typed(CommandRegistryMapper mapper) {
        return new CommandRegistry(mapper);
    }

    public static CommandRegistry untyped() {
        return new CommandRegistry();
    }

    public static CommandRegistry of(String commandName, CommandExecutor onInvoke) {
        return untyped().and(commandName, onInvoke);
    }

    public static <C,E> CommandRegistry ofTyped(
            Class<C> commandType, Class<? extends E> eventType,
            Function<C, Observable<? extends E>> onInvoke,
            CommandRegistryMapper mapper) {
        return typed(mapper).and(commandType, eventType, onInvoke);
    }

    public static CommandRegistry empty() {
        return new CommandRegistry();
    }

    public boolean isEmpty() {
        return commands.isEmpty();
    }

    public Optional<CommandExecutor> findCommand(CommandDescriptor descriptor) {
        return Optional.ofNullable(commands.get(descriptor));
    }

    public Stream<Pair<CommandDescriptor, CommandExecutor>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<CommandDescriptor> streamOfKeys() {
        return stream().map(Pair::getKey);
    }

    @Override
    public String toString() {
        return "CommandRegistry{" +
                "commands=" + commands +
                ", mapper=" + mapper +
                '}';
    }

    @Override
    public Iterator<Pair<CommandDescriptor, CommandExecutor>> iterator() {
        final Iterator<Map.Entry<CommandDescriptor, CommandExecutor>> entryIterator = commands.entrySet().iterator();
        return new Iterator<Pair<CommandDescriptor, CommandExecutor>>() {
            @Override
            public boolean hasNext() {
                return entryIterator.hasNext();
            }

            @Override
            public Pair<CommandDescriptor, CommandExecutor> next() {
                final Map.Entry<CommandDescriptor, CommandExecutor> entry = entryIterator.next();
                return Pair.of(entry.getKey(), entry.getValue());
            }
        };
    }
}
