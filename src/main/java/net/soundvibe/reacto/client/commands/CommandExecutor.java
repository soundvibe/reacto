package net.soundvibe.reacto.client.commands;

import net.soundvibe.reacto.types.*;
import rx.Observable;

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
    Observable<Event> execute(Command command);

}
