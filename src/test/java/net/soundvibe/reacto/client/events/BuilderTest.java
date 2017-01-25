package net.soundvibe.reacto.client.events;

import net.soundvibe.reacto.discovery.types.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.JsonObject;
import org.junit.Test;
import rx.Observable;

import static org.junit.Assert.*;

/**
 * @author Linas on 2017.01.18.
 */
public class BuilderTest {

    @Test
    public void shouldBuildEventHandlerRegistry() throws Exception {
        final EventHandlerRegistry sut = EventHandlerRegistry.Builder.create()
                .register(ServiceType.LOCAL, serviceRecord -> eventHandler())
                .build();

        assertTrue(sut.findFactory(ServiceType.LOCAL).isPresent());
        assertFalse(sut.findFactory(ServiceType.WEBSOCKET).isPresent());
    }

    private EventHandler eventHandler() {
        return new EventHandler() {
            @Override
            public Observable<Event> observe(Command command) {
                return Observable.empty();
            }

            @Override
            public ServiceRecord serviceRecord() {
                return ServiceRecord.create("foo", Status.UP, ServiceType.WEBSOCKET,
                        "id", JsonObject.empty(), JsonObject.empty());
            }
        };
    }
}