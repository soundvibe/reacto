package net.soundvibe.reacto.types;

import java.util.Optional;

/**
 * @author OZY on 2017.01.10.
 */
public final class TypedEvent extends Event {

    private TypedEvent(String name, Optional<MetaData> metaData, Optional<byte[]> payload) {
        super(name, metaData, payload);
    }

    public static TypedEvent create(Class<?> eventType) {
        return new TypedEvent(eventType.getName(), Optional.empty(), Optional.empty());
    }

    public static TypedEvent create(Class<?> eventType, MetaData metaData) {
        return new TypedEvent(eventType.getName(), Optional.of(metaData), Optional.empty());
    }

    public static TypedEvent create(Class<?> eventType, byte[] payload) {
        return new TypedEvent(eventType.getName(), Optional.empty(), Optional.of(payload));
    }

    public static TypedEvent create(Class<?> eventType, MetaData metaData, byte[] payload) {
        return new TypedEvent(eventType.getName(), Optional.of(metaData), Optional.of(payload));
    }

    public String eventType() {
        return name;
    }


}
