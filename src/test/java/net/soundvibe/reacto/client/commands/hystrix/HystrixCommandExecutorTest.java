package net.soundvibe.reacto.client.commands.hystrix;

import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.discovery.LoadBalancers;
import net.soundvibe.reacto.discovery.types.*;
import net.soundvibe.reacto.errors.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.JsonObject;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNotNull;

/**
 * @author Cipolinas on 2016.02.18.
 */
public class HystrixCommandExecutorTest {

    private final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();

    @Test
    public void shouldGetErrorWhenEventHandlersAreEmpty() throws Exception {
        HystrixCommandExecutor sut = new HystrixCommandExecutor(Collections.emptyList(), HystrixCommandExecutor.defaultHystrixSetter);
        sut.execute(Command.create("foo"))
            .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertError(CannotFindEventHandlers.class);
    }

    @Test
    public void shouldExecuteUsingMain() throws Exception {
        final Event expected = Event.create("test");
        HystrixCommandExecutor sut = new HystrixCommandExecutor(singletonList(getEventHandler(Observable.just(expected))),
                HystrixCommandExecutor.defaultHystrixSetter);
        sut.execute(Command.create("foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValue(expected);
    }

    @Test
    public void shouldExecuteUsingMainAndFallback() throws Exception {
        final Event expected = Event.create("test");
        HystrixCommandExecutor sut = new HystrixCommandExecutor(Arrays.asList(
                getEventHandler(Observable.just(expected)),
                getEventHandler(Observable.just(expected))),
                HystrixCommandExecutor.defaultHystrixSetter);
        sut.execute(Command.create("foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValue(expected);
    }

    @Test
    public void shouldExecuteFallback() throws Exception {
        final Event expected = Event.create("test");
        HystrixCommandExecutor sut = new HystrixCommandExecutor(Arrays.asList(
                getEventHandler(Observable.error(new RuntimeException("Error"))),
                getEventHandler(Observable.just(expected))),
                HystrixCommandExecutor.defaultHystrixSetter);
        sut.execute(Command.create("foo"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertValue(expected);
    }

    @Test
    public void shouldCreateUsingFactory() throws Exception {
        final CommandExecutor actual = HystrixCommandExecutor.FACTORY.create(
                Collections.emptyList(),
                LoadBalancers.ROUND_ROBIN,
                null);
        assertNotNull(actual);
    }

    private final ServiceRecord defaultRecord = ServiceRecord.create("hystrix", Status.UP,
            ServiceType.WEBSOCKET, "id", JsonObject.empty(), JsonObject.empty());

    private EventHandler getEventHandler(Observable<Event> event) {
        return new EventHandler() {
            @Override
            public Observable<Event> observe(Command command) {
                return event;
            }
            @Override
            public ServiceRecord serviceRecord() {
                return defaultRecord;
            }
        };
    }
}
