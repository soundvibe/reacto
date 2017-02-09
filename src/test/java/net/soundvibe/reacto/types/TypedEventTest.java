package net.soundvibe.reacto.types;

import org.junit.Test;

import static org.junit.Assert.*;

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
    public void shouldBeCreatedWithMetadata() throws Exception {
        final Event sut = TypedEvent.create(DemoMade.class, MetaData.empty());
        assertNotNull(sut);
    }

    @Test
    public void shouldCreateSimpleEvent() throws Exception {
        final TypedEvent actual = TypedEvent.create(DemoMade.class);
        assertEquals(DemoMade.class.getName(), actual.eventType());
    }
}