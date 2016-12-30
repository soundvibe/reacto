package net.soundvibe.reacto.server;

import io.vertx.core.json.*;
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

    @Test
    public void shouldFindRegisteredService() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                getMetadata()
        );
        assertTrue(ServiceRecords.isService("test", record));
    }

    @Test
    public void shouldNotFindRegisteredService() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                getMetadata()
        );
        assertFalse(ServiceRecords.isService("dummy", record));
    }

    @Test
    public void shouldFindRegisteredCommand() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                getMetadata()
        );
        assertTrue(ServiceRecords.hasCommand("bar", record));
    }

    @Test
    public void shouldNotFindRegisteredCommand() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                getMetadata()
        );
        assertFalse(ServiceRecords.hasCommand("dummy", record));
    }

    @Test
    public void shouldNotFindRegisteredCommandWhenMetadataIsEmpty() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                new JsonObject()
        );
        assertFalse(ServiceRecords.hasCommand("dummy", record));
    }

    private JsonObject getMetadata() {
        return new JsonObject()
                .put(ServiceRecords.LAST_UPDATED, Instant.now().minus(2L, ChronoUnit.MINUTES))
                .put(ServiceRecords.COMMANDS, new JsonArray().add("foo").add("bar"));
    }
}