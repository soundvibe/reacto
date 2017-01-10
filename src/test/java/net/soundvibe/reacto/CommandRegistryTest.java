package net.soundvibe.reacto;

import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.types.*;
import org.junit.Test;
import rx.Observable;

import java.util.function.Function;

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
        final CommandDescriptor descriptor = CommandDescriptor.ofNames("bar", "foo");
        CommandRegistry sut = CommandRegistry.ofTyped(descriptor, o -> Observable.just(Event.create("foo")));
        assertTrue(sut.findCommand(descriptor).isPresent());
    }

    @Test
    public void shouldNotFindCommandWithEventType() throws Exception {
        CommandRegistry sut = CommandRegistry.ofTyped(CommandDescriptor.ofNames("bar", "foo"), o -> Observable.just(Event.create("foo")));
        assertFalse(sut.findCommand(CommandDescriptor.ofNames("bar", "foo2")).isPresent());
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
        for (Pair<CommandDescriptor, Function<Command, Observable<Event>>> pair : sut) {
            assertEquals(CommandDescriptor.of("foo"), pair.getKey());
        }
    }
}
