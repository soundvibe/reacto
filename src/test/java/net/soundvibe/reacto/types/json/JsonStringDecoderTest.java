package net.soundvibe.reacto.types.json;

import org.junit.Test;

import java.io.IOException;

/**
 * @author OZY on 2017.01.19.
 */
public class JsonStringDecoderTest {

    @Test(expected = JsonMapperException.class)
    public void shouldThrow() throws Throwable {
        JsonStringDecoder sut = jsonString -> {
            throw new JsonMapperException("error", new IOException());
        };

        sut.decode("{}");
    }

    @Test(expected = JsonMapperException.class)
    public void shouldThrow2() throws Throwable {
        JsonStringDecoder sut = jsonString -> {
            throw new JsonMapperException("error", new IOException());
        };

        sut.decodeToObject("{}");
    }
}