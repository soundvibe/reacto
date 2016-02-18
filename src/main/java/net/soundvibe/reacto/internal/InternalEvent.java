package net.soundvibe.reacto.internal;

import net.soundvibe.reacto.types.Event;
import net.soundvibe.reacto.types.MetaData;
import net.soundvibe.reacto.mappers.Mappers;
import net.soundvibe.reacto.utils.Bytes;

import java.util.*;

/**
 * @author Linas on 2015.11.13.
 */

public final class InternalEvent {

    public final String name;
    public final Optional<MetaData> metaData;
    public final Optional<byte[]> payload;
    public final EventType eventType;
    public final Optional<Throwable> error;

    InternalEvent(String name, Optional<MetaData> metaData, Optional<byte[]> payload, Optional<Throwable> error, EventType eventType) {
        this.name = name;
        this.metaData = metaData;
        this.payload = payload;
        this.eventType = eventType;
        this.error = error;
    }

    private InternalEvent(Throwable error) {
        this.name = "error";
        this.eventType = EventType.ERROR;
        this.metaData = Optional.empty();
        this.error = Optional.ofNullable(error);
        this.payload = this.error.flatMap(Mappers::exceptionToBytes);
    }

    public static InternalEvent onNext(Event event) {
        return new InternalEvent(event.name, event.metaData, event.payload, Optional.empty(), EventType.NEXT);
    }

    public static InternalEvent onError(Throwable throwable) {
        return new InternalEvent(throwable);
    }

    public static InternalEvent onCompleted() {
        return new InternalEvent("completed", Optional.empty(), Optional.empty(), Optional.empty(), EventType.COMPLETED);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InternalEvent internalEvent = (InternalEvent) o;
        return Objects.equals(name, internalEvent.name) &&
                Objects.equals(metaData, internalEvent.metaData) &&
                Bytes.payloadsAreEqual(this.payload, internalEvent.payload) &&
                eventType == internalEvent.eventType &&
                Objects.equals(error, internalEvent.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, metaData, payload.map(Arrays::hashCode).orElse(0), eventType, error);
    }

    @Override
    public String toString() {
        return "InternalEvent{" +
                "name='" + name + '\'' +
                ", metaData=" + metaData +
                ", payload=" + payload +
                ", eventType=" + eventType +
                ", error=" + error +
                '}';
    }
}
