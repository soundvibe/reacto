package net.soundvibe.reacto.types.json;

import java.time.Instant;
import java.util.*;
import java.util.Optional;
import java.util.stream.*;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Optional.*;

/**
 * Represents immutable JsonObject.
 * JsonObjects can be built using {@link net.soundvibe.reacto.types.json.JsonObjectBuilder}.
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

    public Optional<String> asString(String key) {
        return valueOf(key, String.class);
    }

    public Optional<Integer> asInteger(String key) {
        return valueOf(key, Integer.class);
    }

    public Optional<Long> asLong(String key) {
        return valueOf(key, Long.class);
    }

    public Optional<Double> asDouble(String key) {
        return valueOf(key, Double.class);
    }

    public Optional<Boolean> asBoolean(String key) {
        return valueOf(key, Boolean.class);
    }

    public Optional<Number> asNumber(String key) {
        return valueOf(key, Number.class);
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> valueOf(String key, Class<T> valueClass) {
        if (Enum.class.isAssignableFrom(valueClass)) {
            return asEnum(key, (Class<Enum>) valueClass).map(anEnum -> (T) anEnum);
        } else if (Instant.class.isAssignableFrom(valueClass)) {
            return asInstant(key).map(instant -> (T) instant);
        } else if (byte[].class.isAssignableFrom(valueClass)) {
            return asBytes(key).map(bytes -> (T) bytes);
        }

        return ofNullable(values.get(key))
                .flatMap(o -> valueClass.isInstance(o) ? of(valueClass.cast(o)) : Optional.empty());
    }

    private Optional<byte[]> asBytes(String key) {
        return valueOf(key, String.class)
                .map(s -> Base64.getDecoder().decode(s));
    }

    private Optional<Instant> asInstant(String key) {
        return valueOf(key, String.class)
                .map(s -> Instant.from(ISO_INSTANT.parse(s)));
    }

    @SuppressWarnings("unchecked")
    public Optional<JsonArray> asArray(String key) {
        return Optional.ofNullable(values.get(key))
                .flatMap(o -> o instanceof List ? Optional.of(new JsonArray((List<Object>) o)) :
                        o instanceof JsonArray ? Optional.of((JsonArray)o) : Optional.empty());
    }

    @SuppressWarnings("unchecked")
    public Optional<JsonObject> asObject(String key) {
        return Optional.ofNullable(values.get(key))
                .flatMap(o -> o instanceof Map ? Optional.of(new JsonObject((Map<String, Object>) o)) :
                        o instanceof JsonObject ? Optional.of((JsonObject)o) : Optional.empty());
    }

    public <T extends Enum<T>> Optional<T> asEnum(String key, Class<T> enumClass) {
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

    public boolean hasElements() {
        return !isEmpty();
    }

    public Set<String> fieldNames() {
        return Collections.unmodifiableSet(values.keySet());
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
