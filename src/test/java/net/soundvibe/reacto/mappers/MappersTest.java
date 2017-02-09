package net.soundvibe.reacto.mappers;

import net.soundvibe.reacto.errors.RuntimeProtocolBufferException;
import net.soundvibe.reacto.internal.InternalEvent;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.models.CustomError;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2015.11.23.
 */
public class MappersTest {

    private final byte[] dummyBytes = "dummy".getBytes();

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test
    public void shouldMapExceptions() throws Exception {
        CustomError exception = new CustomError("Not Implemented");
        final Optional<byte[]> bytes = Mappers.exceptionToBytes(exception);
        assertTrue(bytes.orElse(new byte[0]).length > 0);
        final Optional<Throwable> throwable = Mappers.fromBytesToException(bytes.orElse(new byte[0]));
        assertEquals(CustomError.class, throwable.orElseGet(NullPointerException::new).getClass());
        final String message = throwable.map(e -> (CustomError) e)
                .map(customError -> customError.data)
                .orElse("foo");

        assertEquals(exception.getMessage(), message);
    }

    @Test(expected = RuntimeProtocolBufferException.class)
    public void shouldThrowWhenFromBytesToInternalEvent() throws Exception {
        Mappers.fromBytesToInternalEvent(dummyBytes);
    }

    @Test(expected = RuntimeProtocolBufferException.class)
    public void shouldThrowWhenFromBytesToCommand() throws Exception {
        Mappers.fromBytesToCommand(dummyBytes);
    }

    @Test
    public void shouldGetEmptyWhenMappingFromExceptionBytes() throws Exception {
        final Optional<Throwable> actual = Mappers.fromBytesToException(dummyBytes);
        assertEquals(Optional.empty(), actual);
    }

    @Test
    public void shouldMapInternalEvent() throws Exception {
        final InternalEvent expected = InternalEvent.onNext(Event.create("foo"));
        final byte[] bytes = Mappers.internalEventToBytes(expected);

        final InternalEvent actual = Mappers.fromBytesToInternalEvent(bytes);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldMapFromInternalEvent() throws Exception {
        final InternalEvent internalEvent = InternalEvent.onNext(Event.create("foo"));
        final Event event = Mappers.fromInternalEvent(internalEvent);
        assertEquals("foo", event.name);
    }

    @Test
    public void shouldMapCommand() throws Exception {
        final Command expected = Command.create("foo");
        final byte[] bytes = Mappers.commandToBytes(expected);

        final Command actual = Mappers.fromBytesToCommand(bytes);
        assertEquals(expected, actual);
    }
}
