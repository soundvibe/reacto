package net.soundvibe.reacto.types;

import net.soundvibe.reacto.internal.Lazy;
import rx.Observable;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.*;

/**
 * @author OZY on 2016.02.05.
 */
public final class MetaData implements Iterable<Pair<String, String>> {

    private final Iterable<Pair<String, String>> entries;
    private final Supplier<Map<String, String>> mapCache;

    private MetaData(Iterable<Pair<String, String>> entries) {
        this.entries = entries;
        this.mapCache = mapSupplier(entries);
    }

    public static MetaData of(String key, String value) {
        return from(Pair.of(key, value));
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

    @SafeVarargs
    public static MetaData from(Pair<String, String>... pairs) {
        return new MetaData(Arrays.asList(pairs));
    }

    public static MetaData from(Iterable<Pair<String, String>> entries) {
        return new MetaData(entries);
    }

    public static MetaData fromStream(Stream<Pair<String, String>> pairStream) {
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

    public Stream<Pair<String, String>> stream() {
        return StreamSupport.stream(entries.spliterator(), false);
    }

    public Stream<Pair<String, String>> parallelStream() {
        return StreamSupport.stream(entries.spliterator(), true);
    }

    public rx.Observable<Pair<String,String>> toObservable() {
        return Observable.from(this.entries);
    }

    public MetaData concat(MetaData other) {
        return fromStream(Stream.concat(
                StreamSupport.stream(this.spliterator(), false),
                StreamSupport.stream(other.spliterator(), false)));
    }

    private Supplier<Map<String, String>> mapSupplier(Iterable<Pair<String, String>> entries) {
        return Lazy.of(() -> {
            Map<String, String> map = new HashMap<>();
            entries.forEach(pair -> map.put(pair.key, pair.value));
            return map;
        });
    }

    @Override
    public Iterator<Pair<String, String>> iterator() {
        return entries.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MetaData metaData = (MetaData) o;
        return Objects.equals(this.mapCache.get(), metaData.mapCache.get());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.mapCache.get());
    }

    @Override
    public String toString() {
        return "MetaData{" +
                "entries=" + entries +
                ", mapCache=" + mapCache +
                '}';
    }
}
