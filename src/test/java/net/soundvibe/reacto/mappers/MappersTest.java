package net.soundvibe.reacto.mappers;

import net.soundvibe.reacto.client.events.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.models.CustomError;
import org.junit.Test;
import rx.Observable;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2015.11.23.
 */
public class MappersTest {

    @Test
    public void shouldBeBothMainAndFallbackSet() throws Exception {
        final List<EventHandler> actual = Mappers.mapToEventHandlers(
                Nodes.of("localhost", "www.google.com"),
                uri -> arg -> Observable.just(Event.create("foo", Pair.of("arg", "foo " + arg))));

        assertFalse("Mapping should be successful", actual.isEmpty());
        assertNotNull("Main Node should be set", actual.get(0));
        assertNotNull("Fallback Node should be set", actual.get(1));
    }

    @Test
    public void shouldBeOnlyMainSet() throws Exception {
        final List<EventHandler> actual = Mappers.mapToEventHandlers(
                Nodes.of("localhost"),
                uri -> arg -> Observable.just(Event.create("foo", Pair.of("arg", "foo " + arg))));
        assertEquals(1, actual.size());
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
