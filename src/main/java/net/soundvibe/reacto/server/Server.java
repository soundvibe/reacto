package net.soundvibe.reacto.server;

import io.reactivex.Flowable;
import net.soundvibe.reacto.types.Any;

/**
 * @author Cipolinas on 2015.11.16.
 */
public interface Server<T> {

    Flowable<T> start();

    Flowable<Any> stop();

}
