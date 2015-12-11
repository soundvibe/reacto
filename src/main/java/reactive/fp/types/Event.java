package reactive.fp.types;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Linas on 2015.11.13.
 */
@JsonRootName("event")
public class Event<T> implements Serializable, Message<T> {

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    public final T payload;
    public final EventType eventType;

    private Event() {
        this.payload = null;
        this.eventType = EventType.NEXT;
    }

    protected Event(T payload, EventType eventType) {
        this.payload = payload;
        this.eventType = eventType;
    }

    public static <T> Event<T> onNext(T payload) {
        return new Event<>(payload, EventType.NEXT);
    }

    public static Event<Throwable> onError(Throwable throwable) {
        return new Event<>(throwable, EventType.ERROR);
    }

    public static <T> Event<T> onCompleted(T payload) {
        return new Event<>(payload, EventType.COMPLETED);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event<?> event = (Event<?>) o;
        return Objects.equals(payload, event.payload) &&
                eventType == event.eventType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(payload, eventType);
    }

    @Override
    public String toString() {
        return "Event{" +
                "payload=" + payload +
                ", eventType=" + eventType +
                '}';
    }
}
