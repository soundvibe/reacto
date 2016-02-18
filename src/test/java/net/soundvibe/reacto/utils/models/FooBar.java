package net.soundvibe.reacto.utils.models;

import java.util.Objects;

/**
 * @author Cipolinas on 2015.12.01.
 */
public class FooBar extends Foo {

    public final String additionalName;

    private FooBar() {
        super(null);
        this.additionalName = null;
    }

    public FooBar(String name, String additionalName) {
        super(name);
        this.additionalName = additionalName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (!super.equals(o)) return false;
        FooBar fooBar = (FooBar) o;
        return Objects.equals(additionalName, fooBar.additionalName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), additionalName);
    }

    @Override
    public String toString() {
        return "FooBar{" +
                "additionalName='" + additionalName + '\'' +
                '}';
    }
}
