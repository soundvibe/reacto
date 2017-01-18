package net.soundvibe.reacto.types.json;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * @author OZY on 2017.01.18.
 */
public class JsonObjectBuilderTest {

    @Test
    public void shouldBuildJsonObject() throws Exception {
        final JsonObject actual = JsonObjectBuilder.create()
                .put("key", "value1")
                .put("key2", "value2")
                .put("metadata", JsonObjectBuilder.create()
                        .put("sub1", "value3")
                        .put("sub2", "value4")
                        .build()
                )
                .putObject("metadata2", object -> object.put("sub3", "value5"))
                .put("count", 150L)
                .put("gender", Gender.MALE)
                .build();


        System.out.println(actual);
        assertEquals(Optional.of("value1"), actual.valueOf("key", String.class));
        assertEquals(Optional.of("value2"), actual.valueOf("key2", String.class));

        assertEquals(Optional.of(150L), actual.valueOf("count", Long.class));
        assertEquals(Optional.of(Gender.MALE), actual.valueOf("gender", Gender.class));

        assertEquals(Optional.of("value3"), actual.valueOfObject("metadata")
                .orElse(JsonObject.empty())
                .valueOf("sub1", String.class));

        assertEquals(Optional.of("value4"), actual.valueOfObject("metadata")
                .orElse(JsonObject.empty())
                .valueOf("sub2", String.class));

        assertEquals(Optional.of("value5"), actual.valueOfObject("metadata2")
                .orElse(JsonObject.empty())
                .valueOf("sub3", String.class));
    }
}