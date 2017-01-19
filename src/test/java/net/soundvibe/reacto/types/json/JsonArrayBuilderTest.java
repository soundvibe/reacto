package net.soundvibe.reacto.types.json;

import org.junit.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.*;

/**
 * @author OZY on 2017.01.18.
 */
public class JsonArrayBuilderTest {

    @Test
    public void shouldBuildJsonArray() throws Exception {
        final JsonArray actual = JsonArrayBuilder.create()
                .addObject(o -> o.put("key1", "value1").put("key2", "value2"))
                .addObject(o -> o.put("key3", "value3").put("key4", "value4"))
                .add("String")
                .add(15)
                .add(10L)
                .add(100.1)
                .add(true)
                .add(new BigDecimal("4568987458798745856.2"))
                .add("foo".getBytes())
                .add(Instant.MIN)
                .addArray(arr -> arr.add(1))
                .add(Gender.FEMALE)
                .addNull()
                .add(new StringBuilder().append("ok"))
                .build();

        assertEquals("value1", actual.asObject(0).orElse(JsonObject.empty())
                .valueOf("key1", String.class).orElse(""));
        assertEquals("value2", actual.asObject(0).orElse(JsonObject.empty())
                .valueOf("key2", String.class).orElse(""));

        assertEquals("value3", actual.asObject(1).orElse(JsonObject.empty())
                .valueOf("key3", String.class).orElse(""));
        assertEquals("value4", actual.asObject(1).orElse(JsonObject.empty())
                .valueOf("key4", String.class).orElse(""));

        assertEquals(of("String"), actual.asString(2));
        assertEquals(of(15), actual.asInteger(3));
        assertEquals(of(10L), actual.asLong(4));
        assertEquals(of(100.1), actual.asDouble(5));
        assertEquals(of(true), actual.asBoolean(6));
        assertEquals(of(new BigDecimal("4568987458798745856.2")), actual.asNumber(7));
        assertArrayEquals("foo".getBytes(), actual.asBytes(8).orElseThrow(NoSuchElementException::new));
        assertArrayEquals("foo".getBytes(), actual.valueOf(8, byte[].class).orElseThrow(NoSuchElementException::new));
        assertEquals(of(Instant.MIN), actual.asInstant(9));
        assertEquals(of(Instant.MIN), actual.valueOf(9, Instant.class));
        assertEquals(1, actual.asArray(10)
                .flatMap(jsonArray -> jsonArray.asInteger(0)).orElse(0).intValue());
        assertEquals(of(Gender.FEMALE), actual.asEnum(11, Gender.class));
        assertEquals(of(Gender.FEMALE), actual.valueOf(11, Gender.class));
        assertEquals(empty(), actual.valueOf(12, String.class));
        assertEquals(of("ok"), actual.valueOf(13, String.class));

        assertEquals(14, actual.size());
        assertEquals(14L, actual.stream().count());
        assertEquals(14L, actual.parallelStream().count());
        assertEquals(14, actual.toList().size());
        assertFalse(actual.isEmpty());
        assertTrue(actual.hasElements());
    }

    @Test
    public void shouldMerge() throws Exception {
        final JsonArray actual1 = JsonArrayBuilder.create()
                .add(1)
                .add(2)
                .build();

        final JsonArray actual2 = JsonArrayBuilder.create()
                .add(1)
                .add(2)
                .build();

        final JsonArray actual = JsonArrayBuilder.from(actual1).merge(actual2).build();
        assertEquals(4, actual.size());

        final JsonArray actual3 = JsonArrayBuilder.from(actual1).merge(JsonArrayBuilder.from(actual2)).build();
        assertEquals(4, actual3.size());
    }

    @Test
    public void shouldRemove() throws Exception {
        final JsonArray array = JsonArrayBuilder.create()
                .add(1)
                .add(2)
                .build();

        final JsonArray actual = JsonArrayBuilder.from(array)
                .remove(1)
                .build();

        assertEquals(1, actual.size());
        assertEquals(1, actual.asInteger(0).orElse(0).intValue());

        final JsonArray empty = JsonArrayBuilder.from(array)
                .clear()
                .build();

        assertEquals(0, empty.size());
        assertTrue(empty.isEmpty());
        assertFalse(empty.hasElements());
    }

    @Test
    public void shouldBeEqual() throws Exception {
        final JsonArray actual1 = JsonArrayBuilder.create()
                .add(1)
                .add(2)
                .build();

        final JsonArray actual2 = JsonArrayBuilder.create()
                .add(1)
                .add(2)
                .build();

        final JsonArray actual3 = JsonArrayBuilder.create()
                .add(1)
                .add(2)
                .add(3)
                .build();

        assertEquals(actual1, actual2);
        assertNotEquals(actual1, actual3);
        assertEquals(actual1.hashCode(), actual2.hashCode());
    }

    @Test
    public void shouldIterate() throws Exception {
        final JsonArray sut = JsonArrayBuilder.create()
                .add("value1")
                .add("value2")
                .build();

        int count = 0;
        for (final Object o : sut) {
            count++;

            if (count == 1) {
                assertEquals("value1", o);
            } else if (count == 2) {
                assertEquals("value2", o);
            } else {
                fail("Should be at most 2 iterations");
            }
        }
        assertEquals(2, count);
    }

    @Test
    public void shouldIterateWithMapAndList() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("foo", "bar");

        List<Object> list = Arrays.asList(
                Collections.singletonList("foo"),
                map
        );
        final JsonArray sut = new JsonArray(list);
        int count = 0;
        for (Object e : sut) {
            count++;

            if (count == 1) {
                assertEquals(JsonArray.class, e.getClass());
                assertEquals("foo", ((JsonArray) e).asString(0).orElse(""));
            } else if (count == 2) {
                assertEquals(JsonObject.class, e.getClass());
                assertEquals("bar", ((JsonObject) e).asString("foo").orElse(""));
            } else {
                fail("Should be at most 2 iterations");
            }
        }
        assertEquals(2, count);
    }
}