package reactive.fp.client.commands;

import reactive.fp.types.Event;
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
    Observable<Event<?>> execute(final T arg);

    /**
     * Executes command with execution timeout of 1 second
     * @param arg command argument
     * @return event observable
     */
    Observable<Event<?>> observe(final T arg);

    /**
     * Executes command  given execution timeout
     * @param arg command argument
     * @return event observable
     */
    Observable<Event<?>> observe(final T arg, int executionTimeoutInMs);

    default <U> Observable<U> execute(final T arg, Class<U> aClass) {
        return execute(arg)
                .map(objectEvent -> objectEvent.payload)
                .cast(aClass);
    }

    default <U> Observable<U> observe(final T arg, Class<U> aClass) {
        return observe(arg)
                .map(objectEvent -> objectEvent.payload)
                .cast(aClass);
    }

    default <U> Observable<U> observe(final T arg, Class<U> aClass, int executionTimeoutInMs) {
        return observe(arg, executionTimeoutInMs)
                .map(objectEvent -> objectEvent.payload)
                .cast(aClass);
    }

}
