package net.soundvibe.reacto.mappers.jackson;

import com.fasterxml.jackson.annotation.*;

import java.util.Objects;

/**
 * @author Linas on 2017.01.10.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
public abstract class Animal {

    public final String name;

    @JsonCreator
    Animal(@JsonProperty("name") String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Animal animal = (Animal) o;
        return Objects.equals(name, animal.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Animal{" +
                "name='" + name + '\'' +
                '}';
    }
}
