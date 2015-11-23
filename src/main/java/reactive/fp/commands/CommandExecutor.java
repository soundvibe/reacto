package reactive.fp.commands;

import rx.Observable;

/**
 * @author OZY on 2015.11.13.
 */
public interface CommandExecutor<T, U> {

    Observable<U> execute(final T arg);

}
