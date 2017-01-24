package net.soundvibe.reacto.types;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author OZY on 2017.01.24.
 */
public class AnyTest {

    @Test
    public void shouldCreate() throws Exception {
        final Any sut = Any.VOID;
        assertEquals(Any.VOID, sut);
    }
}