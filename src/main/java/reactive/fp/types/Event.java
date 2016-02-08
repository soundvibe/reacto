package reactive.fp.types;

import rx.Observable;

import java.util.Objects;
import java.util.Optional;

/**
 * @author Linas on 2015.11.13.
 */

public final class Event implements Message {

    public final String name;
    public final Optional<MetaData> metaData;
    public final Optional<byte[]> payload;
    public final EventType eventType;
    public final Optional<ReactiveException> error;

    Event(String name, Optional<MetaData> metaData, Optional<byte[]> payload, Optional<Throwable> error, EventType eventType) {
        this.name = name;
        this.metaData = metaData;
        this.payload = payload;
        this.eventType = eventType;
        this.error = error.map(ReactiveException::from);
    }

    Event(Throwable error) {
        this.name = "error";
        this.metaData = Optional.empty();
        this.payload = Optional.empty();
        this.eventType = EventType.ERROR;
        this.error = Optional.ofNullable(error).map(ReactiveException::from);
    }

    public String get(String key) {
        return metaData.map(pairs -> pairs.get(key)).orElse(null);
    }

    public Optional<String> valueOf(String key) {
        return metaData.flatMap(pairs -> pairs.valueOf(key));
    }

    public Observable<Event> toObservable() {
        return Observable.just(this);
    }

    public static Event create(String name, Pair... metaDataPairs) {
        return onNext(name, Optional.of(MetaData.from(metaDataPairs)), Optional.empty());
    }

    public static Event create(String name, MetaData metaData) {
        return onNext(name, Optional.ofNullable(metaData), Optional.empty());
    }

    public static Event create(String name, byte[] payload) {
        return onNext(name, Optional.empty(), Optional.ofNullable(payload));
    }

    public static Event create(String name, MetaData metaData, byte[] payload) {
        return onNext(name, Optional.ofNullable(metaData), Optional.ofNullable(payload));
    }

    public static Event create(String name, Optional<MetaData> metaData, Optional<byte[]> payload) {
        return onNext(name, metaData, payload);
    }

    static Event onNext(String name, Optional<MetaData> metaData, Optional<byte[]> payload) {
        return new Event(name, metaData, payload, Optional.empty(), EventType.NEXT);
    }

    public static Event onError(Throwable throwable) {
        return new Event(throwable);
    }

    public static Event onCompleted() {
        return new Event("completed", Optional.empty(), Optional.empty(), Optional.empty(), EventType.COMPLETED);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(name, event.name) &&
                Objects.equals(metaData, event.metaData) &&
                Objects.equals(payload, event.payload) &&
                eventType == event.eventType &&
                Objects.equals(error, event.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, metaData, payload, eventType, error);
    }

    @Override
    public String toString() {
        return "Event{" +
                "name='" + name + '\'' +
                ", metaData=" + metaData +
                ", payload=" + payload +
                ", eventType=" + eventType +
                ", error=" + error +
                '}';
    }
}
