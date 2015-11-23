package reactive.fp.types;

import rx.Observable;

/**
 * @author Cipolinas on 2015.11.16.
 */
public interface EventHandler<T,U> {

    Observable<U> toObservable(String commandName, T arg);

}
