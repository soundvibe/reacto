package net.soundvibe.reacto.discovery;

import rx.Observable;

/**
 * @author linas on 17.1.9.
 */
public interface ServiceDiscoveryLifecycle<T> {

    Observable<T> startDiscovery(T record);
    Observable<T> closeDiscovery(T record);
    Observable<T> publish(T record);
    Observable<T> cleanServices();

    boolean isOpen();

    default boolean isClosed() {
        return !isOpen();
    }

}
