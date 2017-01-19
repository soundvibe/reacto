package net.soundvibe.reacto.types;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Cipolinas on 2016.02.18.
 */
public class EventTest {

    @Test
    public void shouldBeAbleToFindInSet() throws Exception {
        final Event testEvent = Event.create("test");
        final Event metaEvent = Event.create("test", MetaData.of("one", "two"));
        final Event payloadEvent = Event.create("test", Optional.empty(), Optional.of("data".getBytes()));

        Set<Event> events = new HashSet<>();
        events.add(testEvent);
        events.add(metaEvent);
        events.add(payloadEvent);

        assertTrue("Event with name 'test' not found", events.contains(testEvent));
        assertTrue("Event with name 'test' and metadata not found", events.contains(metaEvent));
        assertTrue("Event with payload not found", events.contains(payloadEvent));
        assertFalse("Should not find because ID's should be different", events.contains(Event.create("test2")));
    }

    @Test
    public void shouldPrintToString() throws Exception {
        final String actual = Event.create("foo").toString();
        assertTrue(actual.startsWith("Event{"));
    }

    @Test
    public void shouldBeCreated() throws Exception {
        final Event one = Event.create("foo", "".getBytes());
        assertNotNull(one);

        final Event two = Event.create("foo", MetaData.empty(), "".getBytes());
        assertNotNull(two);

        final Event three = Event.create("foo", Pair.of("foo", "bar"));
        assertNotNull(three);

        final Optional<String> actual = three.valueOf("foo");
        assertEquals(Optional.of("bar"), actual);
    }
}
