package net.soundvibe.reacto.metric;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author OZY on 2017.01.13.
 */
public class CommandProcessorMetricTest {

    @Test
    public void shouldCalculateAverageExecutionTime() throws Exception {
        CommandProcessorMetric sut = CommandProcessorMetric.of("foo", "bar");
        sut.timeElapsedInMs.set(100L);
        sut.commandCount.set(4);

        final long actual = sut.avgExecutionTimeInMs();
        assertEquals(25L, actual);
    }

    @Test
    public void shouldCalculateCommandsPerSecond() throws Exception {
        CommandProcessorMetric sut = CommandProcessorMetric.of("foo", "bar");
        sut.commandCount.set(4);

        final long actual = sut.commandsPerSecond(1000L);
        assertEquals(4, actual);
        assertEquals(6, sut.commandsPerSecond(600L));
    }

    @Test
    public void shouldCalculateEventsPerSecond() throws Exception {
        CommandProcessorMetric sut = CommandProcessorMetric.of("foo", "bar");
        sut.onNext().onNext().onNext().onNext();

        final long actual = sut.eventsPerSecond(1000L);
        assertEquals(4, actual);
        assertEquals(6, sut.eventsPerSecond(600L));
        assertEquals(0, sut.completed());

        sut.onCompleted();
        assertEquals(1, sut.completed());
    }
}