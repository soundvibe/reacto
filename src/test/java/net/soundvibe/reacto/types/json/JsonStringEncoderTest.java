package net.soundvibe.reacto.types.json;

import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

/**
 * @author OZY on 2017.01.19.
 */
public class JsonStringEncoderTest {

    @Test(expected = JsonMapperException.class)
    public void shouldThrow() throws Throwable {
        JsonStringEncoder sut = map -> {
            throw new JsonMapperException("Error", new IOException());
        };

        sut.encode(Collections.emptyMap());
    }

    @Test(expected = JsonMapperException.class)
    public void shouldThrow2() throws Throwable {
        JsonStringEncoder sut = map -> {
            throw new JsonMapperException("Error", new IOException());
        };

        sut.encode(new JsonObject(Collections.emptyMap()));
    }
}