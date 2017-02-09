package net.soundvibe.reacto.discovery.types;

import net.soundvibe.reacto.server.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.*;
import org.junit.Test;

import java.util.*;

import static net.soundvibe.reacto.discovery.types.ServiceRecord.*;
import static net.soundvibe.reacto.types.CommandDescriptor.COMMAND;
import static net.soundvibe.reacto.types.CommandDescriptor.EVENT;
import static org.junit.Assert.*;

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
        ServiceRecord actual = getWebSocketEndpoint();

        assertEquals("service", actual.name);
        assertEquals(false, actual.location.asBoolean(LOCATION_SSL).orElse(true));
        assertEquals(8181, actual.location.asInteger(LOCATION_PORT).orElse(80).intValue());
        assertEquals("/", actual.location.asString(LOCATION_ROOT).orElse(""));
        assertEquals("1", actual.metadata.asString(METADATA_VERSION).orElse(""));
        JsonArray commands = actual.metadata.asArray(METADATA_COMMANDS).orElse(JsonArray.empty());
        assertEquals(2, commands.size());
        assertEquals("foo", commands.asObject(0).orElse(JsonObject.empty()).asString(COMMAND).orElse(""));
        assertEquals(MakeDemo.class.getName(), commands.asObject(1).orElse(JsonObject.empty()).asString(COMMAND).orElse(""));
        assertEquals("", commands.asObject(0).orElse(JsonObject.empty()).asString(EVENT).orElse("event"));
        assertEquals(DemoMade.class.getName(), commands.asObject(1).orElse(JsonObject.empty()).asString(EVENT).orElse("event"));
    }

    @Test
    public void shouldCreateWebSocketEndpoint2() throws Exception {
        assertNotNull(ServiceRecord.createWebSocketEndpoint(getServiceOptions(), CommandRegistry.empty()));
    }

    @Test
    public void shouldBeEncodedToJsonString() throws Exception {
        ServiceRecord expected = getWebSocketEndpoint();

        final String json = expected.toJson();
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.length() > 10);
        assertTrue(json.contains("\"name\":\"service\""));

        final ServiceRecord actual = ServiceRecord.fromJson(json);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldFallbackToDefaultEnums() throws Exception {
        final String json = getWebSocketEndpoint().toJson();
        final String mutatedJson = json
                .replace("\"status\":\"UP\"", "\"status\":\"NONE\"")
                .replace("\"type\":\"WEBSOCKET\"", "\"type\":\"NONE\"");

        final ServiceRecord actual = ServiceRecord.fromJson(mutatedJson);

        assertEquals(Status.UNKNOWN, actual.status);
        assertEquals(ServiceType.LOCAL, actual.type);
    }

    private ServiceRecord getWebSocketEndpoint() {
        return ServiceRecord.createWebSocketEndpoint(
                getServiceOptions(),
                Arrays.asList(CommandDescriptor.of("foo"), CommandDescriptor.ofTypes(MakeDemo.class, DemoMade.class)));
    }

    private ServiceOptions getServiceOptions() {
        return new ServiceOptions("service", "/", "1", false, 8181);
    }

    @Test
    public void shouldBeCompatibleWithSimpleCommand() throws Exception {
        final TypedCommand command = TypedCommand.create(MakeDemo.class, DemoMade.class, MetaData.empty());
        final TypedCommand command2 = TypedCommand.create(MakeDemo.class, String.class, MetaData.empty());

        ServiceRecord sut = getWebSocketEndpoint();

        assertTrue(sut.isCompatibleWith(command));
        assertFalse(sut.isCompatibleWith(command2));
    }

    @Test
    public void shouldBeCompatibleWithDeserializedCommand() throws Exception {
        final TypedCommand command = TypedCommand.create(MakeDemo.class, DemoMade.class, MetaData.empty());
        final TypedCommand command2 = TypedCommand.create(MakeDemo.class, String.class, MetaData.empty());

        ServiceRecord sut = getWebSocketEndpoint();

        final String json = sut.toJson();

        final ServiceRecord actual = ServiceRecord.fromJson(json);

        assertTrue(actual.isCompatibleWith(command));
        assertFalse(actual.isCompatibleWith(command2));
    }
}