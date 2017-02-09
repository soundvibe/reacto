package net.soundvibe.reacto.errors;

import org.junit.Test;

/**
 * @author OZY on 2017.01.19.
 */
public class ConnectionClosedUnexpectedlyTest {

    @Test(expected = ConnectionClosedUnexpectedly.class)
    public void shouldThrow() throws Exception {
        throw new ConnectionClosedUnexpectedly("error");
    }
}