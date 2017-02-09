package net.soundvibe.reacto.mappers.jackson;

import com.fasterxml.jackson.annotation.*;

import java.util.Objects;

/**
 * @author Linas on 2017.01.10.
 */
public class Feed {

    public final String meal;

    @JsonCreator
    public Feed(@JsonProperty("meal") String meal) {
        this.meal = meal;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Feed feed = (Feed) o;
        return Objects.equals(meal, feed.meal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(meal);
    }

    @Override
    public String toString() {
        return "Feed{" +
                "meal='" + meal + '\'' +
                '}';
    }
}
