package net.soundvibe.reacto.discovery;

import java.util.*;

/**
 * @author OZY on 2016.08.26.
 */
public final class RandomLoadBalancer<T> implements LoadBalancer<T> {

    @Override
    public T balance(List<T> records) {
        return records.get(new Random().nextInt(records.size()));
    }
}
