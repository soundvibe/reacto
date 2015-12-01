package reactive.fp.client.events;

import reactive.fp.types.Event;
import rx.Observable;

/**
 * @author Cipolinas on 2015.11.16.
 */
public interface EventHandler<T> {

    Observable<Event<T>> toObservable(String commandName, Object arg);

}
