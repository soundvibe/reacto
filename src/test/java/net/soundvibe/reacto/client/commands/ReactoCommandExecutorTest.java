package net.soundvibe.reacto.client.commands;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;
import net.soundvibe.reacto.client.events.CommandHandler;
import net.soundvibe.reacto.discovery.LoadBalancers;
import net.soundvibe.reacto.discovery.types.*;
import net.soundvibe.reacto.errors.CannotFindEventHandlers;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.JsonObject;
import org.junit.Test;

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
        final ReactoCommandExecutor sut = new ReactoCommandExecutor(
                Collections.emptyList(), LoadBalancers.ROUND_ROBIN);

        final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.execute(Command.create("new"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotComplete();
        testSubscriber.assertError(CannotFindEventHandlers.class);
    }

    @Test
    public void shouldExecuteAndEmitEvent() throws Exception {
        final ReactoCommandExecutor sut = new ReactoCommandExecutor(singletonList(testHandler(Flowable.just(Event.create("foo")))),
                LoadBalancers.ROUND_ROBIN);

        final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.execute(Command.create("new"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertComplete();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(Event.create("foo"));
    }

    @Test
    public void shouldExecuteAndHandleError() throws Exception {
        List<CommandHandler> commandHandlers = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        commandHandlers.add(testHandler(Flowable.<Event>error(new RuntimeException("error")).doOnError(e -> counter.incrementAndGet())));
        commandHandlers.add(testHandler(Flowable.just(Event.create("foo"))));

        final ReactoCommandExecutor sut = new ReactoCommandExecutor(
                commandHandlers,
                LoadBalancers.ROUND_ROBIN);

        final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.execute(Command.create("new"))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertComplete();
        testSubscriber.assertNoErrors();
        testSubscriber.assertValue(Event.create("foo"));
        assertEquals("Error should have been emitted but was not",1, counter.get());
    }


    private CommandHandler testHandler(Flowable<Event> observable) {
        return new CommandHandler() {
            @Override
            public Flowable<Event> observe(Command command) {
                return observable;
            }

            @Override
            public ServiceRecord serviceRecord() {
                return ServiceRecord.create("test", Status.UP, ServiceType.WEBSOCKET, "1", JsonObject.empty(), JsonObject.empty());
            }
        };
    }
}