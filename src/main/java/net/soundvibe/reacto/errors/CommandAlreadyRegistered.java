package net.soundvibe.reacto.errors;

import net.soundvibe.reacto.types.CommandDescriptor;

/**
 * @author Linas on 2017.01.10.
 */
public class CommandAlreadyRegistered extends RuntimeException {

    public CommandAlreadyRegistered(CommandDescriptor descriptor) {
        super("Command already registered: " + descriptor);
    }
}
