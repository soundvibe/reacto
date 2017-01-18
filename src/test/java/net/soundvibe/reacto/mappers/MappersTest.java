package net.soundvibe.reacto.mappers;

import net.soundvibe.reacto.utils.models.CustomError;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2015.11.23.
 */
public class MappersTest {

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
}
