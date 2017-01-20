package net.soundvibe.reacto.types.json;

import io.vertx.core.json.Json;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

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
                .put("long", 155L)
                .put("double", 155.0)
                .put("boolean", true)
                .put("instant", Instant.MIN)
                .put("bytes", "foo".getBytes())
                .put("bigDecimal", new BigDecimal("4565869874589877.2"))
                .putObject("metadata2", object -> object.put("sub3", "value5"))
                .putArray("array", arr -> arr.add(true).add(false))
                .put("count", 150L)
                .put("gender", Gender.MALE)
                .put("charSequence", new StringBuilder().append("ok"))
                .build();


        System.out.println(actual);
        assertEquals(of("value1"), actual.valueOf("key", String.class));
        assertEquals(of("value2"), actual.valueOf("key2", String.class));

        assertEquals(of(155L), actual.asLong("long"));
        assertEquals(of(155.0), actual.asDouble("double"));
        assertEquals(of(true), actual.asBoolean("boolean"));
        assertEquals(of(Instant.MIN), actual.asInstant("instant"));
        assertArrayEquals("foo".getBytes(), actual.asBytes("bytes").orElseThrow(NoSuchElementException::new));
        assertEquals(of(new BigDecimal("4565869874589877.2")), actual.asNumber("bigDecimal"));


        assertTrue(actual.containsKey("key"));
        assertFalse(actual.containsKey("randomKey-sd"));

        assertFalse(actual.isEmpty());
        assertTrue(actual.hasElements());

        assertEquals(14, actual.size());
        assertNotEquals(0, actual.size());

        assertEquals(of(150L), actual.valueOf("count", Long.class));
        assertEquals(of(Gender.MALE), actual.valueOf("gender", Gender.class));
        assertEquals(of("ok"), actual.valueOf("charSequence", String.class));

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
    public void shouldReturnNewMap() throws Exception {
        final JsonObject sut = JsonObjectBuilder.create()
                .put("key", "value1")
                .build();
        final Map<String, Object> actual = sut.toMap();
        assertEquals(1, actual.size());
        assertEquals("value1", actual.get("key"));

        actual.clear();
        assertEquals("value1", sut.asString("key").orElse(""));
    }

    @Test
    public void shouldReturnNewFieldNamesSet() throws Exception {
        final JsonObject sut = JsonObjectBuilder.create()
                .put("key", "value1")
                .put("key2", "value2")
                .build();
        final Set<String> actual = sut.fieldNames();
        assertEquals(2, actual.size());
        assertEquals(2L, sut.stream().count());
        assertEquals(2L, sut.parallelStream().count());
        assertEquals(2L, sut.streamOfKeys().count());
        assertEquals(2L, sut.streamOfValues().count());
        assertTrue(actual.contains("key"));
        assertTrue(actual.contains("key2"));
        assertFalse(actual.contains("___"));

        actual.clear();
        assertEquals("value1", sut.asString("key").orElse(""));
    }

    @Test
    public void shouldIterateUsingForEach() throws Exception {
        final JsonObject sut = JsonObjectBuilder.create()
                .put("key", "value1")
                .put("key2", "value2")
                .build();

        int count = 0;
        for (final Map.Entry<String, Object> entry : sut) {
            count++;

            if (count == 1) {
                assertEquals("key", entry.getKey());
                assertEquals("value1", entry.getValue());
            } else if (count == 2) {
                assertEquals("key2", entry.getKey());
                assertEquals("value2", entry.getValue());
            } else {
                fail("Should be at most 2 iterations");
            }
        }
        assertEquals(2, count);
    }

    @Test
    public void shouldIterate() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key1", Collections.singletonList("value1"));
        Map<String, Object> subMap = new LinkedHashMap<>();
        subMap.put("foo", "bar");
        map.put("key2", subMap);
        final JsonObject sut = new JsonObject(map);
        int count = 0;
        for (final Map.Entry<String, Object> entry : sut) {
            count++;

            if (count == 1) {
                assertEquals("key1", entry.getKey());
                assertEquals(JsonArray.class, entry.getValue().getClass());
                assertEquals("value1", ((JsonArray) entry.getValue()).asString(0).orElse(""));
            } else if (count == 2) {
                assertEquals("key2", entry.getKey());
                assertEquals(JsonObject.class, entry.getValue().getClass());
                assertEquals("bar", ((JsonObject) entry.getValue()).asString("foo").orElse(""));
            } else {
                fail("Should be at most 2 iterations");
            }
        }
        assertEquals(2, count);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void shouldThrowWhenTryingToMutateItemWhileIterating() throws Exception {
        final Map<String, Object> map = new HashMap<>();
        map.put("foo", Collections.singletonList("bar"));
        final JsonObject actual = JsonObjectBuilder.from(new JsonObject(map))
                .build();

        for (Map.Entry<String, Object> next : actual) {
            next.setValue("value");
        }
    }

    @Test
    public void shouldMerge() throws Exception {
        final JsonObject jsonObject1 = JsonObjectBuilder.create()
                .put("key", "value1")
                .put("key2", "value2")
                .build();

        final JsonObject jsonObject2 = JsonObjectBuilder.create()
                .put("key", "value1")
                .put("key2", "valueNew")
                .put("new", "value3")
                .build();

        final JsonObject merged1 = JsonObjectBuilder.from(jsonObject1)
                .merge(jsonObject2)
                .build();

        assertEquals(3, merged1.size());
        assertEquals("value1", merged1.asString("key").orElse(""));
        assertEquals("valueNew", merged1.asString("key2").orElse(""));
        assertEquals("value3", merged1.asString("new").orElse(""));

        final JsonObject merged2 = JsonObjectBuilder.from(jsonObject1)
                .merge(JsonObjectBuilder.from(jsonObject2))
                .build();

        assertEquals(3, merged2.size());
        assertEquals("value1", merged2.asString("key").orElse(""));
        assertEquals("valueNew", merged2.asString("key2").orElse(""));
        assertEquals("value3", merged2.asString("new").orElse(""));
    }

    @Test
    public void shouldRemove() throws Exception {
        final JsonObject jsonObject = JsonObjectBuilder.create()
                .put("key", "value1")
                .put("key2", "value2")
                .build();

        assertEquals(2, jsonObject.size());

        final JsonObject actual = JsonObjectBuilder.from(jsonObject)
                .remove("key2")
                .build();

        assertEquals(1, actual.size());
        assertEquals("value1", actual.asString("key").orElse(""));

        final JsonObject empty = JsonObjectBuilder.from(jsonObject)
                .clear()
                .build();

        assertEquals(0, empty.size());
        assertTrue(empty.isEmpty());
        assertFalse(empty.hasElements());
    }

    @Test
    public void shouldEncode() throws Exception {
        final JsonObject sut = JsonObjectBuilder.create()
                .put("key", "value1")
                .put("key2", "value2")
                .build();

        final String actual = sut.encode(Object::toString);
        assertEquals(sut.values.toString(), actual);
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

    @Test
    public void shouldBuildFromJsonString() throws Exception {
        final JsonObject actual = JsonObjectBuilder.from("{ \"key1\": \"value1\", \"key2\": \"value2\" }",
                jsonString -> Json.decodeValue(jsonString, Map.class))
                .build();

        assertEquals("value1", actual.asString("key1").orElse(""));
        assertEquals("value2", actual.asString("key2").orElse(""));
    }
}