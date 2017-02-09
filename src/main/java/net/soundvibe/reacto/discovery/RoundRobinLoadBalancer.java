package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.types.Named;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author OZY on 2016.08.26.
 */
public final class RoundRobinLoadBalancer<T extends Named> implements LoadBalancer<T> {

    private final ConcurrentHashMap<String, Integer> cachedRecords = new ConcurrentHashMap<>();

    @Override
    public T balance(List<T> records) {
        if (records.isEmpty()) throw new IllegalArgumentException("No elements to balance");
        return records.get(cachedRecords.compute(records.get(0).name(), (name, lastIndex) -> {
            if (lastIndex == null) {
                return 0;
            }
            final int newIndex = lastIndex + 1;
            return newIndex >= records.size() ? 0 : newIndex;
        }));
    }
}
