package net.soundvibe.reacto.server;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author OZY on 2017.01.19.
 */
public class ServiceOptionsTest {

    @Test
    public void shouldCreateIfSsl() throws Exception {
        final ServiceOptions sut = new ServiceOptions("test", "/", "1", true);
        assertNotNull(sut);

        final ServiceOptions sut2 = new ServiceOptions("test", "/", "1", true);
        assertEquals(sut, sut2);

        assertTrue(sut.toString().startsWith("ServiceOptions{"));
    }
}