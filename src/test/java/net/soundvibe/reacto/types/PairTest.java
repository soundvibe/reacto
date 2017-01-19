package net.soundvibe.reacto.types;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author OZY on 2017.01.19.
 */
public class PairTest {

    @Test
    public void shouldBeTestedForEquality() throws Exception {
        assertEquals(Pair.of("foo", "bar"), Pair.of("foo", "bar"));
        assertNotEquals(Pair.of("foo", "bar"), Pair.of("foo", "bar2"));

        final Pair<String, String> actual = Pair.of("foo", "bar");
        assertEquals("bar", actual.getValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldBeImmutable() throws Exception {
        Pair.of("foo", "bar").setValue("new");
    }
}