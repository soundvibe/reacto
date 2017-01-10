package net.soundvibe.reacto.mappers.jackson;

import com.fasterxml.jackson.annotation.*;

import java.util.Objects;

/**
 * @author Linas on 2017.01.10.
 */
public class JacksonCommand {

    public final String name;

    @JsonCreator
    public JacksonCommand(@JsonProperty("name") String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JacksonCommand command = (JacksonCommand) o;
        return Objects.equals(name, command.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "JacksonCommand{" +
                "name='" + name + '\'' +
                '}';
    }
}
