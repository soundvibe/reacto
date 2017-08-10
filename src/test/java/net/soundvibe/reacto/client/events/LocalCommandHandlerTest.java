package net.soundvibe.reacto.client.events;

import net.soundvibe.reacto.discovery.types.*;
import net.soundvibe.reacto.errors.CannotDiscoverService;
import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.JsonObject;
import org.junit.Test;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;

/**
 * @author OZY on 2017.01.25.
 */
public class LocalCommandHandlerTest {

    @Test
    public void shouldReturnRecord() throws Exception {
        final ServiceRecord expected = getServiceRecord();
        LocalCommandHandler sut = new LocalCommandHandler(expected,
                CommandRegistry.empty());

        final ServiceRecord actual = sut.serviceRecord();
        assertEquals(expected, actual);
    }

    @Test
    public void shouldNotFindService() throws Exception {
        final ServiceRecord expected = getServiceRecord();
        LocalCommandHandler sut = new LocalCommandHandler(expected,
                CommandRegistry.empty());

        TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.observe(Command.create("foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertNoValues();
        testSubscriber.assertError(CannotDiscoverService.class);
    }

    private ServiceRecord getServiceRecord() {
        return ServiceRecord.create(
                "service", Status.UP, ServiceType.LOCAL, "1", JsonObject.empty(), JsonObject.empty());
    }
}