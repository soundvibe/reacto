package net.soundvibe.reacto.types.json;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.stream.*;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Optional.*;

/**
 *
 * Represents immutable JsonArray.
 * JsonArrays can be built using {@link net.soundvibe.reacto.types.json.JsonArrayBuilder}.
 * @author Linas on 2017.01.18.
 */
public final class JsonArray implements Iterable<Object> {

    final List<Object> values;

    private final static JsonArray EMPTY = new JsonArray(Collections.emptyList());

    public JsonArray(List<Object> values) {
        this.values = values;
    }

    public static JsonArray empty() {
        return EMPTY;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> valueOf(int index, Class<T> valueClass) {
        if (Enum.class.isAssignableFrom(valueClass)) {
             return asEnum(index, (Class<Enum>) valueClass).map(anEnum -> (T) anEnum);
        } else if (Instant.class.isAssignableFrom(valueClass)) {
            return asInstant(index).map(instant -> (T) instant);
        } else if (byte[].class.isAssignableFrom(valueClass)) {
            return asBytes(index).map(bytes -> (T) bytes);
        }
        return ofNullable(values.get(index))
                .flatMap(o -> valueClass.isInstance(o) ? of(valueClass.cast(o)) : Optional.empty());
    }

    public Optional<String> asString(int index) {
        return valueOf(index, String.class);
    }

    public Optional<Integer> asInteger(int index) {
        return valueOf(index, Integer.class);
    }

    public Optional<Long> asLong(int index) {
        return valueOf(index, Long.class);
    }

    public Optional<Double> asDouble(int index) {
        return valueOf(index, Double.class);
    }

    public Optional<Boolean> asBoolean(int index) {
        return valueOf(index, Boolean.class);
    }

    public Optional<Number> asNumber(int index) {
        return valueOf(index, Number.class);
    }

    public Optional<byte[]> asBytes(int index) {
        return valueOf(index, String.class)
                .map(s -> Base64.getDecoder().decode(s));
    }

    public Optional<Instant> asInstant(int index) {
        return valueOf(index, String.class)
                .map(s -> Instant.from(ISO_INSTANT.parse(s)));
    }

    @SuppressWarnings("unchecked")
    public Optional<JsonArray> asArray(int index) {
        return ofNullable(values.get(index))
                .flatMap(o -> o instanceof List ? Optional.of(new JsonArray((List<Object>)o)) :
                    o instanceof JsonArray ? Optional.of((JsonArray)o) : Optional.empty());
    }

    @SuppressWarnings("unchecked")
    public Optional<JsonObject> asObject(int index) {
        return ofNullable(values.get(index))
                .flatMap(o -> o instanceof Map ? Optional.of(new JsonObject((Map<String, Object>)o)) :
                        o instanceof JsonObject ? Optional.of((JsonObject)o) : Optional.empty());
    }

    public <T extends Enum<T>> Optional<T> asEnum(int index, Class<T> enumClass) {
        return valueOf(index, String.class)
                .map(name -> Enum.valueOf(enumClass, name));
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public boolean hasElements() {
        return !isEmpty();
    }

    public Stream<Object> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<Object> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    public List<Object> toList() {
        return new ArrayList<>(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (o instanceof List) {
            List list = (List) o;
            return Objects.equals(values, list);
        }
        if (getClass() != o.getClass()) return false;
        JsonArray objects = (JsonArray) o;
        return Objects.equals(values, objects.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public Iterator<Object> iterator() {
        return new JsonArrayIterator(values.iterator());
    }

    @Override
    public String toString() {
        return values.toString();
    }

    private class JsonArrayIterator implements Iterator<Object> {

        private final Iterator<Object> listIterator;

        JsonArrayIterator(Iterator<Object> listIterator) {
            this.listIterator = listIterator;
        }

        @Override
        public boolean hasNext() {
            return listIterator.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Object next() {
            Object next = listIterator.next();
            if (next instanceof Map) {
                next = new JsonObject((Map<String, Object>)next);
            } else if (next instanceof List) {
                next = new JsonArray((List)next);
            }
            return next;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public static class JsonArraySerializer extends JsonSerializer<JsonArray> {
        @Override
        public void serialize(JsonArray value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeObject(value.values);
        }
    }

    public static class JsonArrayDeserializer extends JsonDeserializer<JsonArray> {

        private static final JavaType listType = TypeFactory.defaultInstance().constructCollectionType(
                ArrayList.class, Object.class
        );

        @Override
        public JsonArray deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            return new JsonArray(ctxt.readValue(p, listType));
        }
    }
}
