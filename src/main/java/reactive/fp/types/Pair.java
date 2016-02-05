package reactive.fp.types;

import java.util.Objects;

/**
 * @author OZY on 2016.02.05.
 */
public final class Pair {

    public final String key;
    public final String value;

    public Pair(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public static Pair of(String key, String value) {
        return new Pair(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair pair = (Pair) o;
        return Objects.equals(key, pair.key) &&
                Objects.equals(value, pair.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        return "Pair{" +
                "key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}
