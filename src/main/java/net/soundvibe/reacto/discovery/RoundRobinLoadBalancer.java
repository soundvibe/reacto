package net.soundvibe.reacto.discovery;

import io.vertx.servicediscovery.Record;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author OZY on 2016.08.26.
 */
public final class RoundRobinLoadBalancer implements LoadBalancer {

    private final ConcurrentHashMap<String, Integer> cachedRecords = new ConcurrentHashMap<>();

    @Override
    public Record balance(List<Record> records) {
        return records.get(cachedRecords.compute(records.get(0).getName(), (name, lastIndex) -> {
            if (lastIndex == null) {
                System.out.println("Index of " + name + ": " +  0);
                return 0;
            }
            final int newIndex = lastIndex + 1;
            System.out.println("Index of " + name + ": " + newIndex);
            return newIndex >= records.size() ? 0 : newIndex;
        }));
    }
}
