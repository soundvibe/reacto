package net.soundvibe.reacto.types;

import net.soundvibe.reacto.utils.Bytes;
import rx.Observable;

import java.util.*;

/**
 * @author OZY on 2016.02.08.
 */
public final class Event {

    public final String name;
    public final Optional<MetaData> metaData;
    public final Optional<byte[]> payload;

    Event(String name, Optional<MetaData> metaData, Optional<byte[]> payload) {
        this.name = name;
        this.metaData = metaData;
        this.payload = payload;
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

    public static Event create(String name) {
        return new Event(name, Optional.empty(), Optional.empty());
    }

    @SafeVarargs
    public static Event create(String name, Pair<String, String>... metaDataPairs) {
        return new Event(name, Optional.of(MetaData.from(metaDataPairs)), Optional.empty());
    }

    public static Event create(String name, MetaData metaData) {
        return new Event(name, Optional.ofNullable(metaData), Optional.empty());
    }

    public static Event create(String name, byte[] payload) {
        return new Event(name, Optional.empty(), Optional.ofNullable(payload));
    }

    public static Event create(String name, MetaData metaData, byte[] payload) {
        return new Event(name, Optional.ofNullable(metaData), Optional.ofNullable(payload));
    }

    public static Event create(String name, Optional<MetaData> metaData, Optional<byte[]> payload) {
        return new Event(name, metaData, payload);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return Objects.equals(name, event.name) &&
                Objects.equals(metaData, event.metaData) &&
                Bytes.payloadsAreEqual(this.payload, event.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, metaData, payload.map(Arrays::hashCode).orElse(0));
    }

    @Override
    public String toString() {
        return "Event{" +
                "name='" + name + '\'' +
                ", metaData=" + metaData +
                ", payload=" + payload +
                '}';
    }

}
