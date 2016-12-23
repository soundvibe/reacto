package net.soundvibe.reacto.types;

import java.util.UUID;

/**
 * @author Linas on 2016.12.23.
 */
public interface Named {

    default String name() {
        return UUID.randomUUID().toString();
    }

}
