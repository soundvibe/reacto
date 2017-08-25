package net.soundvibe.reacto.server;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;
import net.soundvibe.reacto.errors.CommandNotFound;
import net.soundvibe.reacto.mappers.Mappers;
import net.soundvibe.reacto.types.*;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * @author OZY on 2017.01.18.
 */
public class CommandProcessorTest {

    @Test
    public void shouldProcessCommandUsingDifferentScheduler() throws Exception {
        final CommandRegistry registry = CommandRegistry.of("foo",
                command -> Flowable.just(Event.create("bar"), Event.create("bar2"))
                        .subscribeOn(Schedulers.io()));
        CommandProcessor sut = new CommandProcessor(registry);
        assertThreadName("RxCachedThreadScheduler", sut);
    }

    @Test
    public void shouldProcessCommandUsingDefaultScheduler() throws Exception {
        final CommandRegistry registry = CommandRegistry.of("foo",
                command -> Flowable.just(Event.create("bar"), Event.create("bar2")));
        CommandProcessor sut = new CommandProcessor(registry);
        assertThreadName("RxComputationThreadPool-", sut);
    }

    @Test
    public void shouldProcessCommandInBytesUsingDefaultScheduler() throws Exception {
        final CommandRegistry registry = CommandRegistry.of("foo",
                command -> Flowable.just(Event.create("bar"), Event.create("bar2")));
        CommandProcessor sut = new CommandProcessor(registry);
        TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.process(Mappers.commandToBytes(Command.create("foo"))).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertComplete();
        testSubscriber.assertValueCount(2);
    }

    @Test
    public void shouldEmitCommandNotFound() throws Exception {
        CommandProcessor sut = new CommandProcessor(CommandRegistry.empty());
        final TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.process(Command.create("foo")).subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotComplete();
        testSubscriber.assertError(CommandNotFound.class);

        final TestSubscriber<Event> testSubscriber2 = new TestSubscriber<>();
        sut.process(Command.create("foo")).subscribe(testSubscriber2);

        testSubscriber2.awaitTerminalEvent();
        testSubscriber2.assertNotComplete();
        testSubscriber2.assertError(CommandNotFound.class);
    }

    @Test
    public void shouldExecutorEmitError() throws Exception {
        final CommandRegistry registry = CommandRegistry.of("foo",
                command -> Flowable.error(new RuntimeException("error")));
        CommandProcessor sut = new CommandProcessor(registry);
        TestSubscriber<Event> testSubscriber = new TestSubscriber<>();
        sut.process(Command.create("foo")).subscribe(testSubscriber);
        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNotComplete();
        testSubscriber.assertError(RuntimeException.class);
    }

    private void assertThreadName(String expected, CommandProcessor sut) {
        final TestSubscriber<String> testSubscriber = new TestSubscriber<>();
        sut.process(Command.create("foo"))
                .map(event -> Thread.currentThread().getName())
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();
        testSubscriber.assertNoErrors();
        testSubscriber.assertComplete();
        final List<String> values = testSubscriber.values();
        assertTrue("Should emit at least one value",values.size() > 0);
        assertTrue("Should use " + expected +" scheduler, but was using: " + values,
                values.stream().allMatch(name -> name.contains(expected)));
    }

}