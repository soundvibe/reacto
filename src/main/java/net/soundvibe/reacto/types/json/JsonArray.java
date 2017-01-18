package net.soundvibe.reacto.types.json;

import java.time.Instant;
import java.util.*;
import java.util.stream.*;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Optional.*;

/**
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

    public <T> Optional<T> valueOf(int index, Class<T> valueClass) {
        if (Enum.class.isAssignableFrom(valueClass)) {
            //noinspection unchecked
            return valueOfEnum(index, (Class<Enum>) valueClass)
                    .map(anEnum -> (T) anEnum);
        }
        return ofNullable(values.get(index))
                .flatMap(o -> valueClass.isInstance(o) ? of(valueClass.cast(o)) : Optional.empty());
    }

    public Optional<byte[]> valueOfBytes(int index) {
        return valueOf(index, String.class)
                .map(s -> Base64.getDecoder().decode(s));
    }

    public Optional<Instant> valueOfInstant(int index) {
        return valueOf(index, String.class)
                .map(s -> Instant.from(ISO_INSTANT.parse(s)));
    }

    @SuppressWarnings("unchecked")
    public Optional<JsonArray> valueOfArray(int index) {
        return ofNullable(values.get(index))
                .flatMap(o -> o instanceof List ? Optional.of(new JsonArray((List<Object>)o)) :
                    o instanceof JsonArray ? Optional.of((JsonArray)o) : Optional.empty());
    }

    @SuppressWarnings("unchecked")
    public Optional<JsonObject> valueOfObject(int index) {
        return ofNullable(values.get(index))
                .flatMap(o -> o instanceof Map ? Optional.of(new JsonObject((Map<String, Object>)o)) :
                        o instanceof JsonObject ? Optional.of((JsonObject)o) : Optional.empty());
    }

    public <T extends Enum<T>> Optional<T> valueOfEnum(int index, Class<T> enumClass) {
        return valueOf(index, String.class)
                .map(name -> Enum.valueOf(enumClass, name));
    }

    public int size() {
        return values.size();
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public Stream<Object> stream() {
        return StreamSupport.stream(spliterator(), false);
    }

    public Stream<Object> parallelStream() {
        return StreamSupport.stream(spliterator(), true);
    }

    public List<Object> toList() {
        return Collections.unmodifiableList(values);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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
            listIterator.remove();
        }
    }
}
