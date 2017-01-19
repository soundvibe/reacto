package net.soundvibe.reacto.internal;

import org.junit.Test;

import java.io.IOException;

/**
 * @author OZY on 2017.01.19.
 */
public class RuntimeProtocolBufferExceptionTest {

    @Test(expected = RuntimeProtocolBufferException.class)
    public void shouldThrow() throws Exception {
        throw new RuntimeProtocolBufferException("error", new IOException("error"));
    }
}