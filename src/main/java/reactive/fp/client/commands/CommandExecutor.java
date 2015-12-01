package reactive.fp.client.commands;

import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public interface CommandExecutor<T> {

    int DEFAULT_EXECUTION_TIMEOUT = 1000;

    /**
     * Executes command without execution timeout
     * @param arg command argument
     * @return event observable
     */
    Observable<T> execute(final Object arg);

    /**
     * Executes command with execution timeout of 1 second
     * @param arg command argument
     * @return event observable
     */
    default Observable<T> observe(final Object arg) {
        return observe(arg, DEFAULT_EXECUTION_TIMEOUT);
    }

    /**
     * Executes command  given execution timeout
     * @param arg command argument
     * @return event observable
     */
    Observable<T> observe(final Object arg, int executionTimeoutInMs);
}
