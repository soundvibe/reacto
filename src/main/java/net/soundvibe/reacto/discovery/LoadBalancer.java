package net.soundvibe.reacto.discovery;

import io.vertx.servicediscovery.Record;

import java.util.List;

/**
 * @author OZY on 2016.08.26.
 */
public interface LoadBalancer {

    Record balance(List<Record> records);

}
