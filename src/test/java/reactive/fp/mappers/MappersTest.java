package reactive.fp.mappers;

import org.junit.Test;
import reactive.fp.client.commands.Nodes;
import reactive.fp.client.events.EventHandlers;
import reactive.fp.types.Event;
import reactive.fp.types.Pair;
import rx.Observable;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2015.11.23.
 */
public class MappersTest {

    @Test
    public void shouldBeBothMainAndFallbackSet() throws Exception {
        final Optional<EventHandlers> actual = Mappers.mapToEventHandlers(
                Nodes.ofMainAndFallback("localhost", "www.google.com"),
                uri -> arg -> Observable.just(Event.create("foo", Pair.of("arg", "foo " + arg)))).apply("foo");

        assertTrue("Mapping should be successful",actual.isPresent());
        final EventHandlers eventHandlers = actual.get();
        assertNotNull("Main Node should be set", eventHandlers.mainNodeClient);
        assertNotNull("Fallback Node should be set", eventHandlers.fallbackNodeClient.get());
    }

    @Test
    public void shouldBeOnlyMainSet() throws Exception {
        final Optional<EventHandlers> actual = Mappers.mapToEventHandlers(
                Nodes.ofMain("localhost"),
                uri -> arg -> Observable.just(Event.create("foo", Pair.of("arg", "foo " + arg)))).apply("foo");

        assertTrue("Mapping should be successful",actual.isPresent());
        final EventHandlers eventHandlers = actual.get();
        assertNotNull("Main Node should be set", eventHandlers.mainNodeClient);
        assertFalse("Fallback Node should not be set", eventHandlers.fallbackNodeClient.isPresent());
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void shouldMapExceptions() throws Exception {
        RuntimeException exception = new RuntimeException("Not Implemented");
        final Optional<byte[]> bytes = Mappers.exceptionToBytes(exception);
        assertTrue(bytes.get().length > 0);
        final Optional<Throwable> throwable = Mappers.fromBytesToException(bytes.get());
        assertEquals(RuntimeException.class, throwable.get().getClass());
        final String message = throwable.map(e -> (RuntimeException) e)
                .map(Throwable::getMessage)
                .orElse("foo");

        assertEquals(exception.getMessage(), message);
    }
}
