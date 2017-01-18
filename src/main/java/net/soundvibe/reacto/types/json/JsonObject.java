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

    private final static JsonObject EMPTY = new JsonObject(Collections.emptyMap());

    final Map<String, Object> values;

    public JsonObject(Map<String, Object> values) {
        this.values = values;
    }

    public static JsonObject empty() {
        return EMPTY;
    }

    public <T> Optional<T> valueOf(String key, Class<T> valueClass) {
        if (Enum.class.isAssignableFrom(valueClass)) {
            //noinspection unchecked
            return valueOfEnum(key, (Class<Enum>) valueClass)
                    .map(anEnum -> (T) anEnum);
        }

        return ofNullable(values.get(key))
                .flatMap(o -> valueClass.isInstance(o) ? of(valueClass.cast(o)) : Optional.empty());
    }

    public Optional<byte[]> valueOfBytes(String key) {
        return valueOf(key, String.class)
                .map(s -> Base64.getDecoder().decode(s));
    }

    public Optional<Instant> valueOfInstant(String key) {
        return valueOf(key, String.class)
                .map(s -> Instant.from(ISO_INSTANT.parse(s)));
    }

    @SuppressWarnings("unchecked")
    public Optional<JsonArray> valueOfArray(String key) {
        return Optional.ofNullable(values.get(key))
                .flatMap(o -> o instanceof List ? Optional.of(new JsonArray((List<Object>) o)) :
                        o instanceof JsonArray ? Optional.of((JsonArray)o) : Optional.empty());
    }

    @SuppressWarnings("unchecked")
    public Optional<JsonObject> valueOfObject(String key) {
        return Optional.ofNullable(values.get(key))
                .flatMap(o -> o instanceof Map ? Optional.of(new JsonObject((Map<String, Object>) o)) :
                        o instanceof JsonObject ? Optional.of((JsonObject)o) : Optional.empty());
    }

    public <T extends Enum<T>> Optional<T> valueOfEnum(String key, Class<T> enumClass) {
        return valueOf(key, String.class)
                .map(name -> Enum.valueOf(enumClass, name));
    }

    public Map<String, Object> toMap() {
        return Collections.unmodifiableMap(values);
    }

    public boolean containsKey(String key) {
        return values.containsKey(key);
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

    @Override
    public String toString() {
        return values.toString();
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
