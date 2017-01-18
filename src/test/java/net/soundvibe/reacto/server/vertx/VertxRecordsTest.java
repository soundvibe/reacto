package net.soundvibe.reacto.server.vertx;

import io.vertx.core.json.*;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpEndpoint;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.models.*;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.Assert.*;

/**
 * @author OZY on 2016.08.26.
 */
public class VertxRecordsTest {

    @Test
    public void shouldBeDown() throws Exception {
        final Record oldRecord = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                new JsonObject().put(VertxRecords.LAST_UPDATED, Instant.now().minus(5L, ChronoUnit.MINUTES)));
        assertTrue(VertxRecords.isDown(oldRecord));
    }

    @Test
    public void shouldBeUp() throws Exception {
        final Record oldRecord = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                new JsonObject().put(VertxRecords.LAST_UPDATED, Instant.now().minus(2L, ChronoUnit.MINUTES)));
        assertFalse(VertxRecords.isDown(oldRecord));
    }

    @Test
    public void shouldFindRegisteredService() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                getMetadata()
        );
        assertTrue(VertxRecords.isService("test", record));
    }

    @Test
    public void shouldNotFindRegisteredService() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                getMetadata()
        );
        assertFalse(VertxRecords.isService("dummy", record));
    }

    @Test
    public void shouldFindRegisteredCommand() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                getMetadata()
        );
        assertTrue(VertxRecords.hasCommand("bar", record));
    }

    @Test
    public void shouldFindRegisteredTypedCommand() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                getMetadata()
        );
        assertTrue(VertxRecords.hasCommand(Foo.class.getName(), FooBar.class.getName(), record));
    }

    @Test
    public void shouldNotFindRegisteredCommand() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                getMetadata()
        );
        assertFalse(VertxRecords.hasCommand("dummy", record));
    }

    @Test
    public void shouldNotFindRegisteredCommandWhenMetadataIsEmpty() throws Exception {
        final Record record = HttpEndpoint.createRecord("test", "localhost", 80, "/",
                new JsonObject()
        );
        assertFalse(VertxRecords.hasCommand("dummy", record));
    }

    private JsonObject getMetadata() {
        return new JsonObject()
                .put(VertxRecords.LAST_UPDATED, Instant.now().minus(2L, ChronoUnit.MINUTES))
                .put(VertxRecords.COMMANDS, new JsonArray()
                        .add(new JsonObject().put(CommandDescriptor.COMMAND, "foo").put(CommandDescriptor.EVENT, ""))
                        .add(new JsonObject().put(CommandDescriptor.COMMAND, "bar").put(CommandDescriptor.EVENT, ""))
                        .add(new JsonObject().put(CommandDescriptor.COMMAND, Foo.class.getName()).put(CommandDescriptor.EVENT, FooBar.class.getName()))
                );
    }
}