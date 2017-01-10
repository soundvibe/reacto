package net.soundvibe.reacto.server;

import net.soundvibe.reacto.types.Any;
import rx.Observable;

/**
 * @author Cipolinas on 2015.11.16.
 */
public interface Server<T> {

    Observable<T> start();

    Observable<Any> stop();

}
