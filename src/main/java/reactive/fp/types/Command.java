package reactive.fp.types;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Cipolinas on 2015.11.16.
 */
public class Command<T> implements Serializable, Message<T> {

    public final ObjectId id;
    public final String name;
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
    public final T payload;

    private Command() {
        this.id = null;
        this.name = null;
        this.payload = null;
    }

    public Command(ObjectId id, String name, T payload) {
        this.id = id;
        this.name = name;
        this.payload = payload;
    }

    public static <T> Command<T> create(String name, T payload) {
        return new Command<>(ObjectId.get(), name, payload);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command<?> command = (Command<?>) o;
        return Objects.equals(id, command.id) &&
                Objects.equals(name, command.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Command{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", payload=" + payload +
                '}';
    }
}
