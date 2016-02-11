package io.reacto.client.events;

import io.reacto.types.Command;
import io.reacto.types.Event;
import rx.Observable;

/**
 * @author Cipolinas on 2015.11.16.
 */
public interface EventHandler {

    Observable<Event> toObservable(Command command);

}
