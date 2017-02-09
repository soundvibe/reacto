package net.soundvibe.reacto.errors;

import org.junit.Test;

import java.io.IOException;

/**
 * @author OZY on 2017.01.19.
 */
public class CannotDiscoverServiceTest {

    @Test(expected = CannotDiscoverService.class)
    public void shouldCreate() throws Exception {
        throw new CannotDiscoverService("error");
    }

    @Test(expected = CannotDiscoverService.class)
    public void shouldCreateWithException() throws Exception {
        throw new CannotDiscoverService("error", new IOException("IO error"));
    }
}