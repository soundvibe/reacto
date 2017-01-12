package net.soundvibe.reacto.metric;

import net.soundvibe.reacto.types.Command;
import org.junit.Test;
import rx.observers.TestSubscriber;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author OZY on 2017.01.12.
 */
public class ReactoDashboardStreamTest {

    @Test
    public void shouldPublishSomeCommandsAndEmitMetricEvents() throws Exception {
        TestSubscriber<CommandHandlerMetrics> testSubscriber = new TestSubscriber<>();
        TestSubscriber<CommandHandlerMetrics> testSubscriber2 = new TestSubscriber<>();

        ReactoDashboardStream.observeCommandHandlers()
                .filter(m -> !m.commands().isEmpty())
                .subscribe(testSubscriber);

        CommandHandlerMetric.of(Command.create("test"))
                .onNext().onNext().onNext()
                .onCompleted();
        CommandHandlerMetric.of(Command.create("foo"))
                .onNext().onNext()
                .onError(new RuntimeException("error"));

        testSubscriber.awaitTerminalEvent(ReactoDashboardStream.DELAY_IN_MS, TimeUnit.MILLISECONDS);
        testSubscriber.assertNoErrors();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertValueCount(1);

        final CommandHandlerMetrics commandHandlerMetrics = testSubscriber.getOnNextEvents().get(0);
        assertTrue(commandHandlerMetrics.memoryUsage().getMax() > 0L);
        assertTrue(commandHandlerMetrics.memoryUsage().getUsed() > 0L);
        assertEquals("Should contain 2 commands but was: " + commandHandlerMetrics.commands(),
                2, commandHandlerMetrics.commands().size());
        assertEquals(3, commandHandlerMetrics.commands().get(0).eventCount());
        assertEquals(2, commandHandlerMetrics.commands().get(1).eventCount());
        assertTrue(commandHandlerMetrics.commands().get(1).hasError());

        ReactoDashboardStream.observeCommandHandlers()
                .filter(m -> !m.commands().isEmpty())
                .subscribe(testSubscriber2);

        CommandHandlerMetric.of(Command.create("new"))
                .onNext().onNext().onNext().onNext()
                .onCompleted();

        testSubscriber.awaitTerminalEventAndUnsubscribeOnTimeout(ReactoDashboardStream.DELAY_IN_MS, TimeUnit.MILLISECONDS);
        testSubscriber.assertNoErrors();
        testSubscriber.assertUnsubscribed();
        testSubscriber.assertValueCount(2);
        final CommandHandlerMetric commandHandlerMetric = testSubscriber.getOnNextEvents().get(1).commands().get(0);
        assertEquals("new", commandHandlerMetric.commandName());
        assertEquals(4, commandHandlerMetric.eventCount());

        testSubscriber2.awaitTerminalEventAndUnsubscribeOnTimeout(ReactoDashboardStream.DELAY_IN_MS, TimeUnit.MILLISECONDS);
        testSubscriber2.assertNoErrors();
        testSubscriber2.assertUnsubscribed();
        testSubscriber2.assertValueCount(1);
        final CommandHandlerMetric commandHandlerMetric2 = testSubscriber2.getOnNextEvents().get(0).commands().get(0);
        assertEquals("new", commandHandlerMetric.commandName());
        assertEquals(4, commandHandlerMetric.eventCount());
    }
}