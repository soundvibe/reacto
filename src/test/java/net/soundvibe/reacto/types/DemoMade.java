package net.soundvibe.reacto.types;

import java.util.Objects;

/**
 * @author OZY on 2017.01.10.
 */
public final class DemoMade {

    public final String text;

    public DemoMade(String text) {
        this.text = text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DemoMade demoMade = (DemoMade) o;
        return Objects.equals(text, demoMade.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    @Override
    public String toString() {
        return "DemoMade{" +
                "text='" + text + '\'' +
                '}';
    }
}
