package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.events.EventHandlerRegistry;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.errors.CannotFindEventHandlers;
import net.soundvibe.reacto.mappers.ServiceRegistryMapper;
import net.soundvibe.reacto.server.ServiceOptions;
import net.soundvibe.reacto.types.*;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.*;

/**
 * @author OZY on 2017.01.19.
 */
public class AbstractServiceRegistryTest {

    @Test
    public void shouldNotFindAnyEventHandlers() throws Exception {
        EventHandlerRegistry registry = EventHandlerRegistry.empty();
        ServiceRegistryMapper mapper = new ServiceRegistryMapper() {
            @Override
            public <C, E> TypedCommand toCommand(C genericCommand, Class<? extends E> eventClass) {
                return null;
            }

            @Override
            public <E> E toGenericEvent(Event event, Class<? extends E> eventClass) {
                return null;
            }
        };
        TestServiceRegistry sut = new TestServiceRegistry(registry, mapper);

        TestSubscriber<CommandExecutor> testSubscriber = new TestSubscriber<>();
        ServiceOptions serviceOptions = new ServiceOptions(
                "test", "/", "1", false, 8080
        );
        sut.findExecutor(Observable.just(Collections.singletonList(
                ServiceRecord.createWebSocketEndpoint(serviceOptions, Collections.emptyList()))),
                "foo", LoadBalancers.ROUND_ROBIN,
                ReactoCommandExecutor.FACTORY)
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(CannotFindEventHandlers.class);
        testSubscriber.assertNotCompleted();
    }


    public class TestServiceRegistry extends AbstractServiceRegistry {

        TestServiceRegistry(EventHandlerRegistry eventHandlerRegistry, ServiceRegistryMapper mapper) {
            super(eventHandlerRegistry, mapper);
        }

        @Override
        public Observable<Any> unpublish(ServiceRecord serviceRecord) {
            return Observable.just(Any.VOID);
        }

        @Override
        protected Observable<List<ServiceRecord>> findRecordsOf(Command command) {
            return null;
        }
    }
}