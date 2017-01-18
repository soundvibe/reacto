package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.types.Any;
import rx.Observable;

/**
 * @author linas on 17.1.9.
 */
public interface ServiceDiscoveryLifecycle {

    Observable<Any> startDiscovery(ServiceRecord serviceRecord, CommandRegistry commandRegistry);
    Observable<Any> closeDiscovery();
}
