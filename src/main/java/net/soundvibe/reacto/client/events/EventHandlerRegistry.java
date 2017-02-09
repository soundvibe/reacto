package net.soundvibe.reacto.client.events;

import net.soundvibe.reacto.discovery.types.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author Linas on 2017.01.18.
 */
public final class EventHandlerRegistry {

    private final Map<ServiceType, Function<ServiceRecord, EventHandler>> handlers;

    private final Map<ServiceRecord, EventHandler> cache;

    private final static EventHandlerRegistry EMPTY = new EventHandlerRegistry(Collections.emptyMap());

    public static EventHandlerRegistry empty() {
        return EMPTY;
    }

    private EventHandlerRegistry(Map<ServiceType, Function<ServiceRecord, EventHandler>> handlers) {
        this.handlers = handlers;
        this.cache = new ConcurrentHashMap<>(handlers.size());
    }

    public Optional<Function<ServiceRecord, EventHandler>> findFactory(ServiceType serviceType) {
        return Optional.ofNullable(handlers.get(serviceType));
    }

    public Stream<EventHandler> find(ServiceRecord serviceRecord) {
        return findFactory(serviceRecord.type)
                .map(factory -> Stream.of(cache.computeIfAbsent(serviceRecord, factory)))
                .orElseGet(Stream::empty);
    }

    public static final class Builder {

        private final Map<ServiceType, Function<ServiceRecord, EventHandler>> handlers = new HashMap<>();

        public static Builder create() {
            return new Builder();
        }

        public Builder register(ServiceType serviceType, Function<ServiceRecord, EventHandler> eventHandlerFactory) {
            handlers.put(serviceType, eventHandlerFactory);
            return this;
        }

        public EventHandlerRegistry build() {
            return new EventHandlerRegistry(this.handlers);
        }


    }


}
