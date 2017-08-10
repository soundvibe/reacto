package net.soundvibe.reacto.discovery;

import io.reactivex.Flowable;
import net.soundvibe.reacto.types.Any;

/**
 * @author linas on 17.1.9.
 */
public interface ServiceDiscoveryLifecycle {

    /**
     * Registers itself in Service Discovery
     * @return Any if registration was successful
     */
    Flowable<Any> register();

    /**
     * Unregisters itself from Service Discovery
     * @return Any if unregistration was successful
     */
    Flowable<Any> unregister();
}
