package io.reacto.client.commands;

import io.reacto.types.Event;
import io.reacto.types.Command;
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
