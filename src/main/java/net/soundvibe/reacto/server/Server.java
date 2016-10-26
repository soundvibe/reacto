package net.soundvibe.reacto.server;

import rx.Observable;

/**
 * @author Cipolinas on 2015.11.16.
 */
public interface Server<T> {

    Observable<T> start();

    Observable<Void> stop();

}
