package net.soundvibe.reacto.internal;

import net.soundvibe.reacto.types.Event;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 * @author Cipolinas on 2016.02.18.
 */
public class InternalEventTest {

    @Test
    public void shouldBeAbleToFindInMaps() throws Exception {
        final RuntimeException error = new RuntimeException("error");

        Set<InternalEvent> internalEvents = new HashSet<>();
        internalEvents.add(InternalEvent.onNext(Event.create("test")));
        internalEvents.add(InternalEvent.onCompleted());
        internalEvents.add(InternalEvent.onError(error));

        assertTrue("onCompleted not found", internalEvents.contains(InternalEvent.onCompleted()));
        assertTrue("onNext not found", internalEvents.contains(InternalEvent.onNext(Event.create("test"))));
        assertTrue("onError not found", internalEvents.contains(InternalEvent.onError(error)));
    }
}
