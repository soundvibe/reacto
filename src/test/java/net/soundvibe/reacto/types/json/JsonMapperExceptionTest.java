package net.soundvibe.reacto.types.json;

import org.junit.Test;

/**
 * @author OZY on 2017.01.19.
 */
public class JsonMapperExceptionTest {

    @Test(expected = JsonMapperException.class)
    public void shouldThrow() throws Exception {
        throw new JsonMapperException("ss", new RuntimeException());

    }
}