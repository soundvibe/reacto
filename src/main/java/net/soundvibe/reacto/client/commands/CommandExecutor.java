package net.soundvibe.reacto.client.commands;

import net.soundvibe.reacto.types.Event;
import net.soundvibe.reacto.types.Command;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public interface CommandExecutor {

    /**
     * Executes command without execution timeout
     * @param command argument
     * @return event observable
     */
    Observable<Event> execute(final Command command);

}
