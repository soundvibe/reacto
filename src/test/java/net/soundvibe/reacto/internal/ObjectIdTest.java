package net.soundvibe.reacto.internal;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/**
 * @author OZY on 2017.01.23.
 */
public class ObjectIdTest {

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenPassedNull() throws Exception {
        new ObjectId(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenHexStringIsNotOfLength24() throws Exception {
        new ObjectId("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowWhenWrongHexString() throws Exception {
        new ObjectId("12345678901234567890123!");
    }

    @Test
    public void shouldBeEqual() throws Exception {
        final ObjectId left = new ObjectId("123456789012345678901230");
        final ObjectId right = new ObjectId("123456789012345678901230");
        assertEquals(left, right);
        assertEquals(left, left);
        assertNotEquals(left, null);
        assertEquals(1, left.compareTo(null));
        assertEquals(0, left.compareTo(right));
    }
}