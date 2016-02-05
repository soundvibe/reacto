package reactive.fp.types;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author OZY on 2016.02.05.
 */
public final class MetaData implements Iterable<Pair> {

    private final Iterable<Pair> entries;

    private MetaData(Iterable<Pair> entries) {
        this.entries = entries;
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

    public Stream<Pair> stream() {
        return StreamSupport.stream(entries.spliterator(), false);
    }

    public Stream<Pair> parallelStream() {
        return StreamSupport.stream(entries.spliterator(), true);
    }

    @Override
    public Iterator<Pair> iterator() {
        return entries.iterator();
    }
}
