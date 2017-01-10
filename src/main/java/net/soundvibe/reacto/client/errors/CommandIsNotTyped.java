package net.soundvibe.reacto.client.errors;

import net.soundvibe.reacto.types.Command;

/**
 * @author OZY on 2017.01.10.
 */
public class CommandIsNotTyped extends RuntimeException {

    public CommandIsNotTyped(Command command) {
        super("Expected typed command but got untyped: " + command);
    }
}
