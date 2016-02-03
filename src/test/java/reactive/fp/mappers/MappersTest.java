package reactive.fp.mappers;

import org.junit.Test;
import reactive.fp.client.commands.CommandDef;
import reactive.fp.types.Event;
import reactive.fp.client.events.EventHandlers;
import rx.Observable;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2015.11.23.
 */
public class MappersTest {

    @Test
    public void shouldBeBothMainAndFallbackSet() throws Exception {
        final Optional<EventHandlers<String, String>> actual = Mappers.mapToEventHandlers(
                CommandDef.ofMainAndFallback("foo", "localhost", "www.google.com", String.class),
                uri -> (commandName, arg) -> Observable.just(Event.onNext("foo " + arg)));

        assertTrue("Mapping should be successful",actual.isPresent());
        final EventHandlers<String, String> eventHandlers = actual.get();
        assertNotNull("Main Node should be set", eventHandlers.mainNodeClient);
        assertNotNull("Fallback Node should be set", eventHandlers.fallbackNodeClient.get());
    }

    @Test
    public void shouldBeOnlyMainSet() throws Exception {
        final Optional<EventHandlers<String, String>> actual = Mappers.mapToEventHandlers(
                CommandDef.ofMain("foo", "localhost", String.class),
                uri -> (commandName, arg) -> Observable.just(Event.onNext("foo " + arg)));

        assertTrue("Mapping should be successful",actual.isPresent());
        final EventHandlers<String, String> eventHandlers = actual.get();
        assertNotNull("Main Node should be set", eventHandlers.mainNodeClient);
        assertFalse("Fallback Node should not be set", eventHandlers.fallbackNodeClient.isPresent());
    }
}
