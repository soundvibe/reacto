package net.soundvibe.reacto.server;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author OZY on 2017.01.19.
 */
public class ServiceOptionsTest {

    @Test
    public void shouldCreate2Params() throws Exception {
        final ServiceOptions sut = new ServiceOptions("test", "/");
        assertEquals("test", sut.serviceName);
        assertEquals("/", sut.root);
        assertFalse(sut.isSsl);
        assertEquals("UNKNOWN", sut.version);
    }

    @Test
    public void shouldCreate3Params() throws Exception {
        final ServiceOptions sut = new ServiceOptions("test", "/", "1.0");
        assertEquals("test", sut.serviceName);
        assertEquals("/", sut.root);
        assertFalse(sut.isSsl);
        assertEquals("1.0", sut.version);
    }

    @Test
    public void shouldCreateIfSsl() throws Exception {
        final ServiceOptions sut = new ServiceOptions("test", "/", "1", true);
        assertNotNull(sut);

        final ServiceOptions sut2 = new ServiceOptions("test", "/", "1", true);
        final ServiceOptions sut3 = new ServiceOptions("test2", "/", "1", true);
        assertEquals(sut, sut2);
        assertEquals(sut.hashCode(), sut2.hashCode());

        assertNotEquals(sut, sut3);
        assertNotEquals(sut.hashCode(), sut3.hashCode());

        assertTrue(sut.toString().startsWith("ServiceOptions{"));
    }
}