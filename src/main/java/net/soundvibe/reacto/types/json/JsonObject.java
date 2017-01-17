package net.soundvibe.reacto.types.json;

import java.time.Instant;
import java.util.*;
import java.util.Optional;
import java.util.stream.*;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Optional.*;

/**
 * @author Linas on 2017.01.18.
 */
public final class JsonObject implements Iterable<Map.Entry<String, Object>> {

    private final Map<String, Object> values;

    public JsonObject(Map<String, Object> values) {
        this.values = values;
    }

    public <T> Optional<T> valueOf(String key, Class<T> valueClass) {
        return ofNullable(values.get(key))
                .flatMap(o -> valueClass.isInstance(o) ? of(valueClass.cast(o)) : empty());
    }

    public Optional<byte[]> valueOfBytes(String key) {
        return valueOf(key, String.class)
                .map(s -> Base64.getDecoder().decode(s));
    }

    public Optional<Instant> valueOfInstant(String key) {
        return valueOf(key, String.class)
                .map(s -> Instant.from(ISO_INSTANT.parse(s)));
    }

    public Optional<JsonArray> valueOfArray(String key) {
        return valueOf(key, List.class)
                .map(JsonArray::new);
    }

    public Optional<JsonObject> valueOfObject(String key) {
        return valueOf(key, Map.class)
                .map(JsonObject::new);
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Stream<Map.Entry<String, Object>> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<String> streamOfKeys() {
        return stream().map(Map.Entry::getKey);
    }

    public Stream<Object> streamOfValues() {
        return stream().map(Map.Entry::getValue);
    }

    public Stream<Map.Entry<String, Object>> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsonObject entries = (JsonObject) o;
        return Objects.equals(values, entries.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return new JsonObjectIterator(values.entrySet().iterator());
    }

    private class JsonObjectIterator implements Iterator<Map.Entry<String, Object>> {

        private final Iterator<Map.Entry<String, Object>> entryIterator;

        JsonObjectIterator(Iterator<Map.Entry<String, Object>> entryIterator) {
            this.entryIterator = entryIterator;
        }

        @Override
        public boolean hasNext() {
            return entryIterator.hasNext();
        }

        @SuppressWarnings("unchecked")
        @Override
        public Map.Entry<String, Object> next() {
            Map.Entry<String, Object> next = entryIterator.next();
            if (next.getValue() instanceof Map) {
                return new Entry(next.getKey(), new JsonObject((Map<String,Object>)next.getValue()));
            } else if (next.getValue() instanceof List) {
                return new Entry(next.getKey(), new JsonArray((List) next.getValue()));
            }
            return next;
        }

        @Override
        public void remove() {
            entryIterator.remove();
        }
    }

    private static final class Entry implements Map.Entry<String, Object> {
        private final String key;
        private final Object value;

        Entry(String key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public Object setValue(Object value) {
            throw new UnsupportedOperationException();
        }
    }
}
