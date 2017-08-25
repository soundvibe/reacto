package net.soundvibe.reacto.client.events;

import io.reactivex.Flowable;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.types.*;

/**
 * @author Cipolinas on 2015.11.16.
 */
public interface CommandHandler extends Named {

    Flowable<Event> observe(Command command);

    ServiceRecord serviceRecord();

}
