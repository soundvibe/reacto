package net.soundvibe.reacto.mappers.jackson;

import com.fasterxml.jackson.annotation.*;

/**
 * @author Linas on 2017.01.10.
 */
public class Cat extends Animal {

    @JsonCreator
    public Cat(@JsonProperty("name") String name) {
        super(name);
    }
}
