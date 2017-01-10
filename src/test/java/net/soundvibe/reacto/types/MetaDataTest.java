package net.soundvibe.reacto.types;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author OZY on 2017.01.10.
 */
public class MetaDataTest {

    @Test
    public void shouldConcatTwoMetadataInstances() throws Exception {
        final MetaData first = MetaData.of("foo", "bar");
        final MetaData second = first.concat(MetaData.of("key", "value"));

        assertEquals("bar", second.get("foo"));
        assertEquals("value", second.get("key"));
        assertFalse(second.valueOf("unknown").isPresent());
    }
}