package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.types.Named;

import java.util.Objects;

/**
 * @author Linas on 2016.12.23.
 */
public class Name implements Named {

    public final String name;
    public final String surname;

    public Name(String name, String surname) {
        this.name = name;
        this.surname = surname;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Name name1 = (Name) o;
        return Objects.equals(name, name1.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
