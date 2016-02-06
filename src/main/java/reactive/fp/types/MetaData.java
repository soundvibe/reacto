package reactive.fp.types;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
}
