package reactive.fp.types;

import reactive.fp.internal.Lazy;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.*;

/**
 * @author OZY on 2016.02.05.
 */
public final class MetaData implements Iterable<Pair> {

    private final Iterable<Pair> entries;
    private final Supplier<Map<String, String>> mapCache;

    private MetaData(Iterable<Pair> entries) {
        this.entries = entries;
        this.mapCache = mapSupplier(entries);
    }

    public static MetaData of(String key, String value) {
        return MetaData.from(Pair.of(key, value));
    }

    public static MetaData of(String key1, String value1, String key2, String value2) {
        return MetaData.from(Pair.of(key1, value1), Pair.of(key2, value2));
    }

    public static MetaData of(String key1, String value1, String key2, String value2, String key3, String value3) {
        return MetaData.from(Pair.of(key1, value1), Pair.of(key2, value2), Pair.of(key3, value3));
    }

    public static MetaData of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4) {
        return MetaData.from(Pair.of(key1, value1), Pair.of(key2, value2), Pair.of(key3, value3), Pair.of(key4, value4));
    }

    public static MetaData of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4,
                              String key5, String value5) {
        return MetaData.from(Pair.of(key1, value1), Pair.of(key2, value2), Pair.of(key3, value3), Pair.of(key4, value4),
                Pair.of(key5, value5));
    }

    public static MetaData of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4,
                                   String key5, String value5, String key6, String value6) {
        return MetaData.from(Pair.of(key1, value1), Pair.of(key2, value2), Pair.of(key3, value3), Pair.of(key4, value4),
                Pair.of(key5, value5), Pair.of(key6, value6));
    }

    public static MetaData of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4,
                              String key5, String value5, String key6, String value6, String key7, String value7) {
        return MetaData.from(Pair.of(key1, value1), Pair.of(key2, value2), Pair.of(key3, value3), Pair.of(key4, value4),
                Pair.of(key5, value5), Pair.of(key6, value6), Pair.of(key7, value7));
    }

    public static MetaData of(String key1, String value1, String key2, String value2, String key3, String value3, String key4, String value4,
                              String key5, String value5, String key6, String value6, String key7, String value7, String key8, String value8) {
        return MetaData.from(Pair.of(key1, value1), Pair.of(key2, value2), Pair.of(key3, value3), Pair.of(key4, value4),
                Pair.of(key5, value5), Pair.of(key6, value6), Pair.of(key7, value7), Pair.of(key8, value8));
    }

    public static MetaData from(Pair... pairs) {
        return new MetaData(Arrays.asList(pairs));
    }

    public static MetaData from(Iterable<Pair> entries) {
        return new MetaData(entries);
    }

    public static MetaData fromStream(Stream<Pair> pairStream) {
        return new MetaData(pairStream.collect(Collectors.toList()));
    }

    public static MetaData fromMap(Map<String, String> map) {
        return new MetaData(map.entrySet().stream()
                .map(entry -> Pair.of(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
    }

    public String get(String key) {
        return mapCache.get().get(key);
    }

    public Optional<String> valueOf(String key) {
        return Optional.ofNullable(mapCache.get().get(key));
    }

    public Stream<Pair> stream() {
        return StreamSupport.stream(entries.spliterator(), false);
    }

    public Stream<Pair> parallelStream() {
        return StreamSupport.stream(entries.spliterator(), true);
    }

    private Supplier<Map<String, String>> mapSupplier(Iterable<Pair> entries) {
        return Lazy.of(() -> {
            Map<String, String> map = new HashMap<>();
            entries.forEach(pair -> map.put(pair.key, pair.value));
            return map;
        });
    }

    @Override
    public Iterator<Pair> iterator() {
        return entries.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetaData metaData = (MetaData) o;
        return Objects.equals(entries, metaData.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entries);
    }

    @Override
    public String toString() {
        return "MetaData{" +
                "entries=" + entries +
                ", mapCache=" + mapCache +
                '}';
    }
}
