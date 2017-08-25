package net.soundvibe.reacto;

import io.reactivex.Flowable;
import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.errors.CommandAlreadyRegistered;
import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.DemoCommandRegistryMapper;
import net.soundvibe.reacto.utils.models.FooBar;
import org.junit.Test;

import java.util.*;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

/**
 * @author Linas on 2015.11.13.
 */
public class CommandRegistryTest {

    @Test
    public void shouldFindCommand() throws Exception {
        CommandRegistry sut = CommandRegistry.of("foo", o -> Flowable.just(Event.create("foo")));

        assertTrue(sut.findCommand(CommandDescriptor.of("foo")).isPresent());
    }

    @Test
    public void shouldFindCommandWithEventType() throws Exception {
        final CommandDescriptor descriptor = CommandDescriptor.ofTypes(MakeDemo.class, DemoMade.class);
        CommandRegistry sut = CommandRegistry.ofTyped(
                MakeDemo.class, DemoMade.class,
                makeDemo -> Flowable.just(new DemoMade("foo")),
                new DemoCommandRegistryMapper());
        assertTrue(sut.findCommand(descriptor).isPresent());
    }

    @Test
    public void shouldNotFindCommandWithEventType() throws Exception {
        CommandRegistry sut = CommandRegistry.ofTyped(
                MakeDemo.class, DemoMade.class,
                makeDemo -> Flowable.just(new DemoMade("foo")),
                new DemoCommandRegistryMapper());
        assertFalse(sut.findCommand(CommandDescriptor.ofTypes(MakeDemo.class, FooBar.class)).isPresent());
    }

    @Test
    public void shouldNotFindCommand() throws Exception {
        assertFalse(CommandRegistry.of("dfdf", o -> Flowable.empty()).findCommand(CommandDescriptor.of("foobar")).isPresent());
    }

    @Test
    public void shouldStreamOverCommands() throws Exception {
        final long actual = CommandRegistry.of("foo", o -> Flowable.just(Event.create("foo")))
                .stream()
                .count();
        assertEquals(1L, actual);
    }

    @Test
    public void shouldLoopOverCommands() throws Exception {
        final CommandRegistry sut = CommandRegistry.of("foo", o -> Flowable.just(Event.create("foo")));
        for (Pair<CommandDescriptor, CommandExecutor> pair : sut) {
            assertEquals(CommandDescriptor.of("foo"), pair.getKey());
        }
    }

    @Test(expected = CommandAlreadyRegistered.class)
    public void shouldDisallowDuplicatedCommands() throws Exception {
        CommandRegistry
                .of("foo", o -> Flowable.empty())
                .and("foo", o -> Flowable.empty());
    }

    @Test(expected = CommandAlreadyRegistered.class)
    public void shouldDisallowDuplicatedTypedCommands() throws Exception {
        CommandRegistry
                .ofTyped(MakeDemo.class, DemoMade.class, o -> Flowable.empty(), new DemoCommandRegistryMapper())
                .and(MakeDemo.class, DemoMade.class, o -> Flowable.empty());
    }

    @Test
    public void shouldAllowSameTypedCommandsButDifferentEvents() throws Exception {
        final CommandRegistry sut = CommandRegistry
                .ofTyped(MakeDemo.class, DemoMade.class, o -> Flowable.empty(), new DemoCommandRegistryMapper())
                .and(MakeDemo.class, FooBar.class, o -> Flowable.empty());

        assertEquals(2L, sut.stream().count());
    }

    @Test
    public void shouldPrintToString() throws Exception {
        final CommandRegistry actual = CommandRegistry.empty();
        assertTrue(actual.toString().startsWith("CommandRegistry{"));
    }

    @Test
    public void shouldGetKeyStream() throws Exception {
        CommandExecutor commandExecutor = command -> Flowable.empty();
        CommandRegistry commandRegistry = CommandRegistry.of("one", commandExecutor).and("two", commandExecutor);
        List<CommandDescriptor> actual = commandRegistry.streamOfKeys()
                .sorted(comparing(commandDescriptor -> commandDescriptor.commandType))
                .collect(toList());
        assertEquals(Arrays.asList(CommandDescriptor.of("one"), CommandDescriptor.of("two")), actual);
    }

    @Test
    public void shouldBeEmpty() throws Exception {
        assertTrue(CommandRegistry.empty().isEmpty());
    }

    @Test
    public void shouldNotBeEmpty() throws Exception {
        assertFalse(CommandRegistry.of("foo", command -> Flowable.empty()).isEmpty());
    }
}
