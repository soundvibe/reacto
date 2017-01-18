package net.soundvibe.reacto.types.json;

import org.junit.Test;

import java.time.Instant;
import java.util.Optional;

import static java.util.Optional.of;
import static org.junit.Assert.*;

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
                .putArray("array", arr -> arr.add(true).add(false))
                .put("count", 150L)
                .put("gender", Gender.MALE)
                .build();


        System.out.println(actual);
        assertEquals(of("value1"), actual.valueOf("key", String.class));
        assertEquals(of("value2"), actual.valueOf("key2", String.class));

        assertEquals(of(150L), actual.valueOf("count", Long.class));
        assertEquals(of(Gender.MALE), actual.valueOf("gender", Gender.class));

        assertEquals(of("value3"), actual.asObject("metadata")
                .orElse(JsonObject.empty())
                .valueOf("sub1", String.class));

        assertEquals(of("value4"), actual.asObject("metadata")
                .orElse(JsonObject.empty())
                .valueOf("sub2", String.class));

        assertEquals(of("value5"), actual.asObject("metadata2")
                .orElse(JsonObject.empty())
                .valueOf("sub3", String.class));

        assertEquals(of(true), actual.asArray("array").orElse(JsonArray.empty()).valueOf(0, Boolean.class));
        assertEquals(of(false), actual.asArray("array").orElse(JsonArray.empty()).valueOf(1, Boolean.class));
    }

    @Test
    public void shouldGetBytesValue() throws Exception {
        final JsonObject actual = JsonObjectBuilder.create()
                .put("payload", "someData".getBytes())
                .build();

        assertArrayEquals("someData".getBytes(), actual.valueOf("payload", byte[].class).orElse(new byte[0]));
    }

    @Test
    public void shouldGetInstantValue() throws Exception {
        final Instant now = Instant.now();
        final JsonObject actual = JsonObjectBuilder.create()
                .put("timestamp", now)
                .build();

        assertEquals(now, actual.valueOf("timestamp", Instant.class).orElse(Instant.MIN));
    }

    @Test
    public void shouldGetEnumValue() throws Exception {
        final JsonObject actual = JsonObjectBuilder.create()
                .put("gender", Gender.MALE)
                .build();

        assertEquals(Gender.MALE, actual.valueOf("gender", Gender.class).orElse(Gender.FEMALE));
    }

    @Test
    public void shouldGetNull() throws Exception {
        final JsonObject actual = JsonObjectBuilder.create()
                .putNull("foo")
                .build();

        assertEquals(Optional.empty(), actual.valueOf("foo", String.class));
    }
}