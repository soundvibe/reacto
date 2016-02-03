package reactive.fp.client.commands;

import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public interface CommandExecutor<T> {

    /**
     * Executes command without execution timeout
     * @param arg command argument
     * @return event observable
     */
    Observable<T> execute(final Object arg);

}
