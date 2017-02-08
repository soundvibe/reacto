package net.soundvibe.reacto.internal;

import net.soundvibe.reacto.types.Pair;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * @author OZY on 2016.02.02.
 */
public final class ExpiringCache<T, U> implements Cache<T, U> {

    private final long durationInMs;
    private final Map<T, Pair<U, Long>> cache = new ConcurrentHashMap<>();

    private ExpiringCache(long expireIn, TimeUnit timeUnit) {
        this.durationInMs = timeUnit.toMillis(expireIn);
    }

    /**
     * Constructs a cache which should expire periodically after specified expireIn period
     * @param expireIn expiration period
     * @param timeUnit time unit of expiration period
     * @return ExpiringCache instance
     */
    public static <T,U> ExpiringCache<T,U> periodically(long expireIn, TimeUnit timeUnit) {
        return new ExpiringCache<>(expireIn, timeUnit);
    }

    @Override
    public U computeIfAbsent(T key,
                             Function<? super T, ? extends U> mappingFunction) {
        return cache.compute(key, (k, v) -> v == null || System.currentTimeMillis() - v.value >= durationInMs ?
                Pair.of(mappingFunction.apply(k), System.currentTimeMillis()) :
                v
        ).getKey();
    }
}
