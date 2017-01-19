package net.soundvibe.reacto.types;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author OZY on 2017.01.19.
 */
public class NamedTest {

    @Test
    public void shouldReturnDefaultName() throws Exception {
        final Named named = new Named() {};
        assertNotNull(named.name());
    }
}