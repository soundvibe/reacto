package net.soundvibe.reacto.client.commands;

import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.discovery.*;
import net.soundvibe.reacto.discovery.types.*;
import net.soundvibe.reacto.errors.CannotFindEventHandlers;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.JsonObject;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

/**
 * @author OZY on 2017.01.24.
 */
public class ReactoCommandExecutorTest {

    @Test
    public void shouldNotFindEventHandlers() throws Exception {
        final ReactoCommandExecutor sut = new ReactoCommandExecutor(Collections.emptyList(), LoadBalancers.ROUND_ROBIN,
                new TestRegistry<>(Observable.empty()));

        final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.execute(Command.create("new"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertError(CannotFindEventHandlers.class);
    }

    @Test
    public void shouldExecuteAndEmitEvent() throws Exception {
        final ReactoCommandExecutor sut = new ReactoCommandExecutor(singletonList(testHandler(Observable.just(Event.create("foo")))),
                LoadBalancers.ROUND_ROBIN,
                new TestRegistry<>(Observable.just(Event.create("foo"))));

        final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.execute(Command.create("new"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(Event.create("foo"));
    }

    @Test
    public void shouldExecuteAndHandleError() throws Exception {
        List<EventHandler> eventHandlers = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        eventHandlers.add(testHandler(Observable.<Event>error(new RuntimeException("error")).doOnError(e -> counter.incrementAndGet())));
        eventHandlers.add(testHandler(Observable.just(Event.create("foo"))));

        final ReactoCommandExecutor sut = new ReactoCommandExecutor(
                eventHandlers,
                LoadBalancers.ROUND_ROBIN,
                new TestRegistry<>(Observable.empty()));

        final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.execute(Command.create("new"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertCompleted();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(Event.create("foo"));
        assertEquals("Error should have been emitted but was not",1, counter.get());
    }


    private EventHandler testHandler(Observable<Event> observable) {
        return new EventHandler() {
            @Override
            public Observable<Event> observe(Command command) {
                return observable;
            }

            @Override
            public ServiceRecord serviceRecord() {
                return ServiceRecord.create("test", Status.UP, ServiceType.WEBSOCKET, "1", JsonObject.empty(), JsonObject.empty());
            }
        };
    }

    private class TestRegistry<E extends Event> implements ServiceRegistry {

        private Observable<? extends E> events;

        private TestRegistry(Observable<? extends E> events) {
            this.events = events;
        }

        @Override
        public <E, C> Observable<E> execute(C command,
                                            Class<? extends E> eventClass,
                                            LoadBalancer<EventHandler> loadBalancer,
                                            CommandExecutorFactory commandExecutorFactory) {
            return (Observable<E>) events;
        }

        @Override
        public Observable<Any> unpublish(ServiceRecord serviceRecord) {
            return Observable.just(Any.VOID);
        }
    }
}