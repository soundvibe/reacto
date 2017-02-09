package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.types.Any;
import rx.Observable;

/**
 * @author linas on 17.1.9.
 */
public interface ServiceDiscoveryLifecycle {

    /**
     * Registers itself in Service Discovery
     * @return Any if registration was successful
     */
    Observable<Any> register();

    /**
     * Unregisters itself from Service Discovery
     * @return Any if unregistration was successful
     */
    Observable<Any> unregister();
}
