package reactive.fp.client.events;

import reactive.fp.types.Command;
import reactive.fp.types.Event;
import rx.Observable;

/**
 * @author Cipolinas on 2015.11.16.
 */
public interface EventHandler {

    Observable<Event> toObservable(Command command);

}
