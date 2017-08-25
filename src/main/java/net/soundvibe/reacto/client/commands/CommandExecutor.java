package net.soundvibe.reacto.client.commands;

import io.reactivex.Flowable;
import net.soundvibe.reacto.types.*;

/**
 * @author OZY on 2015.11.13.
 */
@FunctionalInterface
public interface CommandExecutor {

    /**
     * Executes command without execution timeout
     * @param command argument
     * @return event observable
     */
    Flowable<Event> execute(Command command);

}
