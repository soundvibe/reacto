package net.soundvibe.reacto;

import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.errors.CommandAlreadyRegistered;
import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.DemoCommandRegistryMapper;
import net.soundvibe.reacto.utils.models.FooBar;
import org.junit.Test;
import rx.Observable;

import static org.junit.Assert.*;

/**
 * @author Linas on 2015.11.13.
 */
public class CommandRegistryTest {

    @Test
    public void shouldFindCommand() throws Exception {
        CommandRegistry sut = CommandRegistry.of("foo", o -> Observable.just(Event.create("foo")));

        assertTrue(sut.findCommand(CommandDescriptor.of("foo")).isPresent());
    }

    @Test
    public void shouldFindCommandWithEventType() throws Exception {
        final CommandDescriptor descriptor = CommandDescriptor.ofTypes(MakeDemo.class, DemoMade.class);
        CommandRegistry sut = CommandRegistry.ofTyped(
                MakeDemo.class, DemoMade.class,
                makeDemo -> Observable.just(new DemoMade("foo")),
                new DemoCommandRegistryMapper());
        assertTrue(sut.findCommand(descriptor).isPresent());
    }

    @Test
    public void shouldNotFindCommandWithEventType() throws Exception {
        CommandRegistry sut = CommandRegistry.ofTyped(
                MakeDemo.class, DemoMade.class,
                makeDemo -> Observable.just(new DemoMade("foo")),
                new DemoCommandRegistryMapper());
        assertFalse(sut.findCommand(CommandDescriptor.ofTypes(MakeDemo.class, FooBar.class)).isPresent());
    }

    @Test
    public void shouldNotFindCommand() throws Exception {
        assertFalse(CommandRegistry.of("dfdf", o -> Observable.empty()).findCommand(CommandDescriptor.of("foobar")).isPresent());
    }

    @Test
    public void shouldStreamOverCommands() throws Exception {
        final long actual = CommandRegistry.of("foo", o -> Observable.just(Event.create("foo")))
                .stream()
                .count();
        assertEquals(1L, actual);
    }

    @Test
    public void shouldLoopOverCommands() throws Exception {
        final CommandRegistry sut = CommandRegistry.of("foo", o -> Observable.just(Event.create("foo")));
        for (Pair<CommandDescriptor, CommandExecutor> pair : sut) {
            assertEquals(CommandDescriptor.of("foo"), pair.getKey());
        }
    }

    @Test(expected = CommandAlreadyRegistered.class)
    public void shouldDisallowDuplicatedCommands() throws Exception {
        CommandRegistry
                .of("foo", o -> Observable.empty())
                .and("foo", o -> Observable.empty());
    }

    @Test(expected = CommandAlreadyRegistered.class)
    public void shouldDisallowDuplicatedTypedCommands() throws Exception {
        CommandRegistry
                .ofTyped(MakeDemo.class, DemoMade.class, o -> Observable.empty(), new DemoCommandRegistryMapper())
                .and(MakeDemo.class, DemoMade.class, o -> Observable.empty());
    }

    @Test
    public void shouldAllowSameTypedCommandsButDifferentEvents() throws Exception {
        final CommandRegistry sut = CommandRegistry
                .ofTyped(MakeDemo.class, DemoMade.class, o -> Observable.empty(), new DemoCommandRegistryMapper())
                .and(MakeDemo.class, FooBar.class, o -> Observable.empty());

        assertEquals(2L, sut.stream().count());
    }

    @Test
    public void shouldPrintToString() throws Exception {
        final CommandRegistry actual = CommandRegistry.empty();
        assertTrue(actual.toString().startsWith("CommandRegistry{"));
    }
}
