package net.soundvibe.reacto.mappers.jackson;

import com.fasterxml.jackson.annotation.*;

import java.util.Objects;

/**
 * @author Linas on 2017.01.10.
 */
public class JacksonEvent {

    public final String name;

    @JsonCreator
    public JacksonEvent(@JsonProperty("name") String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JacksonEvent that = (JacksonEvent) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "JacksonEvent{" +
                "name='" + name + '\'' +
                '}';
    }
}
