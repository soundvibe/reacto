package net.soundvibe.reacto.mappers;

import net.soundvibe.reacto.utils.models.CustomError;
import net.soundvibe.reacto.client.events.EventHandlers;
import net.soundvibe.reacto.types.Event;
import net.soundvibe.reacto.types.Pair;
import org.junit.Test;
import net.soundvibe.reacto.client.commands.Nodes;
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
                uri -> arg -> Observable.just(Event.create("foo", Pair.of("arg", "foo " + arg)))).get();

        assertTrue("Mapping should be successful",actual.isPresent());
        final EventHandlers eventHandlers = actual.get();
        assertNotNull("Main Node should be set", eventHandlers.mainNodeClient);
        assertNotNull("Fallback Node should be set", eventHandlers.fallbackNodeClient.get());
    }

    @Test
    public void shouldBeOnlyMainSet() throws Exception {
        final Optional<EventHandlers> actual = Mappers.mapToEventHandlers(
                Nodes.ofMain("localhost"),
                uri -> arg -> Observable.just(Event.create("foo", Pair.of("arg", "foo " + arg)))).get();

        assertTrue("Mapping should be successful",actual.isPresent());
        final EventHandlers eventHandlers = actual.get();
        assertNotNull("Main Node should be set", eventHandlers.mainNodeClient);
        assertFalse("Fallback Node should not be set", eventHandlers.fallbackNodeClient.isPresent());
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void shouldMapExceptions() throws Exception {
        CustomError exception = new CustomError("Not Implemented");
        final Optional<byte[]> bytes = Mappers.exceptionToBytes(exception);
        assertTrue(bytes.get().length > 0);
        final Optional<Throwable> throwable = Mappers.fromBytesToException(bytes.get());
        assertEquals(CustomError.class, throwable.get().getClass());
        final String message = throwable.map(e -> (CustomError) e)
                .map(customError -> customError.data)
                .orElse("foo");

        assertEquals(exception.getMessage(), message);
    }
}
