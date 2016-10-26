package net.soundvibe.reacto.discovery;

import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author OZY on 2016.08.26.
 */
public interface LoadBalancersTestUtils {

    Record record1 = HttpEndpoint.createRecord("test", "localhost/1");
    Record record2 = HttpEndpoint.createRecord("test", "localhost/2");
    Record record3 = HttpEndpoint.createRecord("test", "localhost/3");
    Record record4 = HttpEndpoint.createRecord("test", "localhost/4");
    Record record5 = HttpEndpoint.createRecord("test", "localhost/5");

    static void assertRecords(Record expected, Record actual) {
        assertEquals(expected.toJson(), actual.toJson());
    }

    static void assertOneOf(Record expected, List<Record> ofRecords) {
        assertTrue("None of " + ofRecords + " matches " + expected, ofRecords.stream()
                .anyMatch(record -> expected.toJson().equals(record.toJson())));
    }

}
