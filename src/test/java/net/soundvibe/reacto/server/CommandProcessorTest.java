package net.soundvibe.reacto.server;

import net.soundvibe.reacto.errors.CommandNotFound;
import net.soundvibe.reacto.types.*;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author OZY on 2017.01.18.
 */
public class CommandProcessorTest {

    @Test
    public void shouldProcessCommandUsingDifferentScheduler() throws Exception {
        final CommandRegistry registry = CommandRegistry.of("foo",
                command -> Observable.just(Event.create("bar"), Event.create("bar2"))
                        .subscribeOn(Schedulers.computation()));
        CommandProcessor sut = new CommandProcessor(registry);
        assertThreadName("computation", sut);
    }

    @Test
    public void shouldProcessCommandUsingDefaultScheduler() throws Exception {
        final CommandRegistry registry = CommandRegistry.of("foo",
                command -> Observable.just(Event.create("bar"), Event.create("bar2")));
        CommandProcessor sut = new CommandProcessor(registry);
        assertThreadName("pool-", sut);
    }

    @Test
    public void shouldEmitCommandNotFound() throws Exception {
        CommandProcessor sut = new CommandProcessor(CommandRegistry.empty());
        final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.process(Command.create("foo")).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertError(CommandNotFound.class);

        final TestSubscriber<Event> testSubscriber2 = new TestSubscriber<>();
        sut.execute(Command.create("foo")).subscribe(testSubscriber2);

        testSubscriber2.awaitTerminalEvent();
        testSubscriber2.assertNotCompleted();
        testSubscriber2.assertError(CommandNotFound.class);
    }

    private void assertThreadName(String expected, CommandProcessor sut) {
        final TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        sut.process(Command.create("foo"))
                .map(event -> Thread.currentThread().getName())
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertCompleted();
        final List<String> values = testSubscriber.getOnNextEvents();
        assertTrue("Should emit at least one value",values.size() > 0);
        assertTrue("Should use " + expected +" scheduler, but was using: " + values,
                values.stream().allMatch(name -> name.toLowerCase().contains(expected)));
    }

}