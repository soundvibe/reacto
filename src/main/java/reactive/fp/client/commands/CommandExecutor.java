package reactive.fp.client.commands;

import reactive.fp.types.Command;
import reactive.fp.types.Event;
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
