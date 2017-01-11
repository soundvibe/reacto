package net.soundvibe.reacto.utils.models;

import java.util.Objects;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class Foo {
    public final String name;

    private Foo() {
        this.name = null;
    }

    public Foo(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final Foo foo = (Foo) o;
        return Objects.equals(name, foo.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "Foo{" +
                "name='" + name + '\'' +
                '}';
    }
}
