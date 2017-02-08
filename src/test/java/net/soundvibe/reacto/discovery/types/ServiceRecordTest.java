package net.soundvibe.reacto.discovery.types;

import net.soundvibe.reacto.server.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.*;
import org.junit.Test;

import java.util.*;

import static net.soundvibe.reacto.discovery.types.ServiceRecord.*;
import static net.soundvibe.reacto.types.CommandDescriptor.COMMAND;
import static net.soundvibe.reacto.types.CommandDescriptor.EVENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Linas on 2017.01.18.
 */
public class ServiceRecordTest {

    @Test
    public void shouldPrintToString() throws Exception {
        final ServiceRecord sut = getServiceRecord();

        final String actual = sut.toString();
        assertTrue(actual.length() > 10);
    }

    @Test
    public void shouldBeEqual() throws Exception {
        assertEquals(getServiceRecord(), getServiceRecord());

        final ServiceRecord expected = ServiceRecord.create("foo", Status.UP, ServiceType.WEBSOCKET, "id",
                JsonObjectBuilder.create()
                        .putArray("key1", a -> a.add("val1"))
                        .build(),
                JsonObjectBuilder.create()
                        .put("foo", "bar")
                        .build());

        final ServiceRecord serviceRecord = ServiceRecord.create("foo", Status.UP, ServiceType.WEBSOCKET, "id",
                new JsonObject(createMap("key1", Collections.singletonList("val1"))),
                new JsonObject(createMap("foo", "bar")));

        assertEquals(expected, serviceRecord);
    }

    private Map<String, Object> createMap(String key, Object value) {
        final Map<String, Object> map = new HashMap<>(1);
        map.put(key, value);
        return map;
    }

    private ServiceRecord getServiceRecord() {
        return ServiceRecord.create("foo", Status.UP, ServiceType.WEBSOCKET, "id",
                JsonObjectBuilder.create()
                        .put("key1", "value1")
                        .build(),
                JsonObjectBuilder.create()
                        .put("foo", "bar")
                        .build());
    }

    @Test
    public void shouldCreateWebSocketEndpoint() throws Exception {
        ServiceRecord actual = ServiceRecord.createWebSocketEndpoint(
                new ServiceOptions("service", "/", "1", false, 8181),
                Arrays.asList(CommandDescriptor.of("foo"), CommandDescriptor.ofTypes(MakeDemo.class, DemoMade.class)));

        assertEquals("service", actual.name);
        assertEquals(false, actual.location.asBoolean(LOCATION_SSL).orElse(true));
        assertEquals(8181, actual.location.asInteger(LOCATION_PORT).orElse(80).intValue());
        assertEquals("/", actual.location.asString(LOCATION_ROOT).orElse(""));
        assertEquals("1", actual.metaData.asString(METADATA_VERSION).orElse(""));
        JsonArray commands = actual.metaData.asArray(METADATA_COMMANDS).orElse(JsonArray.empty());
        assertEquals(2, commands.size());
        assertEquals("foo", commands.asObject(0).orElse(JsonObject.empty()).asString(COMMAND).orElse(""));
        assertEquals(MakeDemo.class.getName(), commands.asObject(1).orElse(JsonObject.empty()).asString(COMMAND).orElse(""));
        assertEquals("", commands.asObject(0).orElse(JsonObject.empty()).asString(EVENT).orElse("event"));
        assertEquals(DemoMade.class.getName(), commands.asObject(1).orElse(JsonObject.empty()).asString(EVENT).orElse("event"));
    }
}