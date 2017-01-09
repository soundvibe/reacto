package net.soundvibe.reacto.discovery;

import io.vertx.servicediscovery.Record;
import rx.Observable;

/**
 * @author linas on 17.1.9.
 */
public interface ServiceDiscoveryLifecycle {

    Observable<Record> startDiscovery(Record record);
    Observable<Record> closeDiscovery(Record record);
    Observable<Record> publish(Record record);
    Observable<Record> cleanServices();

    boolean isOpen();

    default boolean isClosed() {
        return !isOpen();
    }

}
