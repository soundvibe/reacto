package net.soundvibe.reacto.types;

import java.util.Objects;

/**
 * @author OZY on 2017.01.10.
 */
public final class DemoMade {

    public final String name;

    public DemoMade(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DemoMade demoMade = (DemoMade) o;
        return Objects.equals(name, demoMade.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "DemoMade{" +
                "name='" + name + '\'' +
                '}';
    }
}
