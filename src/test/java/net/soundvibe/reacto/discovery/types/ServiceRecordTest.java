package net.soundvibe.reacto.discovery.types;

import net.soundvibe.reacto.types.json.JsonObjectBuilder;
import org.junit.Test;

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


}