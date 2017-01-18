package net.soundvibe.reacto.types.json;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author OZY on 2017.01.18.
 */
public class JsonArrayBuilderTest {

    @Test
    public void shouldBuildJsonArray() throws Exception {
        final JsonArray actual = JsonArrayBuilder.create()
                .addObject(o -> o.put("key1", "value1").put("key2", "value2"))
                .addObject(o -> o.put("key3", "value3").put("key4", "value4"))
                .build();

        assertEquals("value1", actual.valueOfObject(0).orElse(JsonObject.empty())
                .valueOf("key1", String.class).orElse(""));
        assertEquals("value2", actual.valueOfObject(0).orElse(JsonObject.empty())
                .valueOf("key2", String.class).orElse(""));

        assertEquals("value3", actual.valueOfObject(1).orElse(JsonObject.empty())
                .valueOf("key3", String.class).orElse(""));
        assertEquals("value4", actual.valueOfObject(1).orElse(JsonObject.empty())
                .valueOf("key4", String.class).orElse(""));
    }
}