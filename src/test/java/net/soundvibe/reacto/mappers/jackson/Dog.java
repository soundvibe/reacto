package net.soundvibe.reacto.mappers.jackson;

import com.fasterxml.jackson.annotation.*;

/**
 * @author Linas on 2017.01.10.
 */
public class Dog extends Animal {

    @JsonCreator
    public Dog(@JsonProperty("name") String name) {
        super(name);
    }

}
