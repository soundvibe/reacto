package net.soundvibe.reacto.utils;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2015.11.23.
 */
public class WebUtilsTest {

    @Test
    public void shouldEndWithDelimiter() throws Exception {
        final String actual = WebUtils.includeEndDelimiter("localhost");
        assertEquals("localhost/", actual);
    }

    @Test
    public void shouldNotEndWithDelimiter() throws Exception {
        final String actual = WebUtils.excludeEndDelimiter("localhost/");
        assertEquals("localhost", actual);
    }

    @Test
    public void shouldNotEndWithDelimiterWhenNoEndDelimiter() throws Exception {
        final String actual = WebUtils.excludeEndDelimiter("localhost");
        assertEquals("localhost", actual);
    }

    @Test
    public void shouldStartWithDelimiter() throws Exception {
        final String actual = WebUtils.includeStartDelimiter("foo");
        assertEquals("/foo", actual);
    }

    @Test
    public void shouldNotStartWithDelimiter() throws Exception {
        final String actual = WebUtils.excludeStartDelimiter("/foo");
        assertEquals("foo", actual);
    }

    @Test
    public void shouldNotStartWithDelimiterWhenNoStartDelimiter() throws Exception {
        final String actual = WebUtils.excludeStartDelimiter("foo");
        assertEquals("foo", actual);
    }

    @Test
    public void shouldResolveWebSocketUri() throws Exception {
        final URI actual = WebUtils.resolveWsURI("http://localhost:8080/foo");
        assertEquals(URI.create("ws://localhost:8080/foo"), actual);
    }

    @Test
    public void shouldResolveWebSocketSUri() throws Exception {
        final URI actual = WebUtils.resolveWsURI("https://localhost:8080/foo");
        assertEquals(URI.create("wss://localhost:8080/foo"), actual);
    }

    @Test
    public void shouldResolveWebSocketFromWebSocketUrl() throws Exception {
        final URI actual = WebUtils.resolveWsURI("ws://localhost/foo");
        assertEquals(URI.create("ws://localhost/foo"), actual);
    }

    @Test
    public void shouldReturnLocalAddress() throws Exception {
        final String actual = WebUtils.getLocalAddress();
        assertNotNull(actual);
    }
}
