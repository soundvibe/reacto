package net.soundvibe.reacto.server;

import io.vertx.core.json.JsonObject;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

/**
 * @author OZY on 2016.08.26.
 */
public class ServiceRecordsTest {

    @Test
    public void shouldBeDown() throws Exception {
        final Record oldRecord = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                new JsonObject().put(ServiceRecords.LAST_UPDATED, Instant.now().minus(5L, ChronoUnit.MINUTES)));
        assertTrue(ServiceRecords.isDown(oldRecord));
    }

    @Test
    public void shouldBeUp() throws Exception {
        final Record oldRecord = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                new JsonObject().put(ServiceRecords.LAST_UPDATED, Instant.now().minus(2L, ChronoUnit.MINUTES)));
        assertFalse(ServiceRecords.isDown(oldRecord));
    }
}