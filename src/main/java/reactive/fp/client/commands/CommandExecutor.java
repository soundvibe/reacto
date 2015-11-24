package reactive.fp.client.commands;

import reactive.fp.types.Event;
import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public interface CommandExecutor<T> {

    Observable<Event<?>> execute(final T arg);

    default <U> Observable<U> execute(final T arg, Class<U> aClass) {
        return execute(arg)
                .map(objectEvent -> objectEvent.payload)
                .cast(aClass);
    }

}
