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

    public static final long OBSERVE_DELAY = 100L;
    public static final long WAIT_DELAY = 200L;
    public static final String CMD_ONE = "one111";

    @Test
    public void shouldPublishSomeCommandsAndEmitMetricEvents() throws Exception {
        TestSubscriber<CommandProcessorMetrics> testSubscriber = new TestSubscriber<>();
        TestSubscriber<CommandProcessorMetrics> testSubscriber2 = new TestSubscriber<>();

        ReactoDashboardStream.observeCommandHandlers(OBSERVE_DELAY, TimeUnit.MILLISECONDS)
                .filter(m -> !m.commands().isEmpty())
                .subscribe(testSubscriber);

        CommandProcessorMetric.of(Command.create("test"))
                .onNext().onNext().onNext()
                .onCompleted();
        CommandProcessorMetric.of(Command.create("foo"))
                .onNext().onNext()
                .onError(new RuntimeException("error"));

        testSubscriber.awaitTerminalEvent(WAIT_DELAY, TimeUnit.MILLISECONDS);
        testSubscriber.assertNoErrors();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertValueCount(1);

        final CommandProcessorMetrics commandProcessorMetrics = testSubscriber.getOnNextEvents().get(0);
        assertTrue(commandProcessorMetrics.memoryUsage().getMax() > 0L);
        assertTrue(commandProcessorMetrics.memoryUsage().getUsed() > 0L);
        assertEquals("Should contain 2 commands but was: " + commandProcessorMetrics.commands(),
                2, commandProcessorMetrics.commands().size());
        assertEquals(2, commandProcessorMetrics.getCommand(0).eventCount());
        assertEquals(1, commandProcessorMetrics.getCommand(0).errors());
        assertEquals(3, commandProcessorMetrics.getCommand(1).eventCount());


        ReactoDashboardStream.observeCommandHandlers(OBSERVE_DELAY, TimeUnit.MILLISECONDS)
                .filter(m -> !m.commands().isEmpty())
                .subscribe(testSubscriber2);

        CommandProcessorMetric.of(Command.create("new"))
                .onNext().onNext().onNext().onNext()
                .onCompleted();

        testSubscriber.awaitTerminalEventAndUnsubscribeOnTimeout(WAIT_DELAY, TimeUnit.MILLISECONDS);
        testSubscriber.assertNoErrors();
        testSubscriber.assertUnsubscribed();
        testSubscriber.assertValueCount(2);
        final CommandProcessorMetric commandProcessorMetric = testSubscriber.getOnNextEvents().get(1).getCommand(0);
        assertEquals("new", commandProcessorMetric.commandName());
        assertEquals(4, commandProcessorMetric.eventCount());

        testSubscriber2.awaitTerminalEventAndUnsubscribeOnTimeout(WAIT_DELAY, TimeUnit.MILLISECONDS);
        testSubscriber2.assertNoErrors();
        testSubscriber2.assertUnsubscribed();
        testSubscriber2.assertValueCount(1);
        final CommandProcessorMetric commandProcessorMetric2 = testSubscriber2.getOnNextEvents().get(0).getCommand(0);
        assertEquals("new", commandProcessorMetric.commandName());
        assertEquals(4, commandProcessorMetric.eventCount());
    }

    @Test
    public void shouldAggregateCommands() throws Exception {
        TestSubscriber<CommandProcessorMetrics> testSubscriber = new TestSubscriber<>();
        ReactoDashboardStream.observeCommandHandlers(OBSERVE_DELAY, TimeUnit.MILLISECONDS)
                .filter(m -> !m.commands().isEmpty())
                .subscribe(testSubscriber);

        CommandProcessorMetric.of(Command.create(CMD_ONE))
                .onNext().onNext().onNext()
                .onCompleted();
        CommandProcessorMetric.of(Command.create("two"))
                .onNext()
                .onCompleted();
        CommandProcessorMetric.of(Command.create(CMD_ONE))
                .onNext().onNext()
                .onCompleted();

        testSubscriber.awaitTerminalEventAndUnsubscribeOnTimeout(WAIT_DELAY, TimeUnit.MILLISECONDS);
        testSubscriber.assertNoErrors();
        testSubscriber.assertNotCompleted();
        testSubscriber.assertValueCount(1);
        final CommandProcessorMetrics commandProcessorMetrics = testSubscriber.getOnNextEvents().get(0);

        assertEquals("Should be 2 commands",2, commandProcessorMetrics.commands().size());

        assertEquals(CMD_ONE, commandProcessorMetrics.getCommand(0).commandName());
        assertEquals(commandProcessorMetrics.getCommand(0).toString(),5, commandProcessorMetrics.getCommand(0).eventCount());

        assertEquals("two", commandProcessorMetrics.getCommand(1).commandName());
        assertEquals(1, commandProcessorMetrics.getCommand(1).eventCount());
    }

    @Test
    public void shouldAggregateManyCallsOfSingleCommand() throws Exception {
        TestSubscriber<CommandProcessorMetrics> testSubscriber = new TestSubscriber<>();
        ReactoDashboardStream.observeCommandHandlers(OBSERVE_DELAY, TimeUnit.MILLISECONDS)
                .filter(m -> !m.commands().isEmpty())
                .subscribe(testSubscriber);

        for (int i = 0; i < 100; i++) {
            final CommandProcessorMetric metric = CommandProcessorMetric.of(Command.create(CMD_ONE));
            Thread.sleep(1L);
            metric.onNext().onNext().onNext().onCompleted();
        }
        testSubscriber.awaitTerminalEventAndUnsubscribeOnTimeout(WAIT_DELAY, TimeUnit.MILLISECONDS);
        testSubscriber.assertNoErrors();
        testSubscriber.assertNotCompleted();
        final CommandProcessorMetrics commandProcessorMetrics = testSubscriber.getOnNextEvents().get(0);

        assertEquals("Should be 1 actual",1, commandProcessorMetrics.commands().size());
        final CommandProcessorMetric actual = commandProcessorMetrics.getCommand(0);
        assertEquals(CMD_ONE, actual.commandName());
        assertTrue(actual.toString(), actual.eventCount() > 0);

        assertEquals(0, actual.errors());
        assertTrue("Was " + actual.avgExecutionTimeInMs(),actual.avgExecutionTimeInMs() > 0L);
        assertTrue("Was " + actual.totalExecutionTimeInMs(),actual.totalExecutionTimeInMs() > 0L);
    }
}