package net.soundvibe.reacto.discovery;

import io.vertx.servicediscovery.Record;

import java.util.*;

/**
 * @author OZY on 2016.08.26.
 */
public final class RandomLoadBalancer implements LoadBalancer {

    @Override
    public Record balance(List<Record> records) {
        return records.get(new Random().nextInt(records.size()));
    }
}
