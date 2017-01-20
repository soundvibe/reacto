package net.soundvibe.reacto.types;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author OZY on 2017.01.20.
 */
public class TypedEventTest {

    @Test
    public void shouldCreateEventWithPayload() throws Exception {
        final TypedEvent actual = TypedEvent.create(DemoMade.class, MetaData.of("key", "value"), "payload".getBytes());
        assertEquals(DemoMade.class.getName(), actual.eventType());
        assertEquals("value", actual.get("key"));
    }

    @Test
    public void shouldCreateSimpleEvent() throws Exception {
        final TypedEvent actual = TypedEvent.create(DemoMade.class);
        assertEquals(DemoMade.class.getName(), actual.eventType());
    }
}