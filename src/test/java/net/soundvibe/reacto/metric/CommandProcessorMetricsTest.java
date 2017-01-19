package net.soundvibe.reacto.metric;

import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertTrue;

/**
 * @author OZY on 2017.01.19.
 */
public class CommandProcessorMetricsTest {

    @Test(expected = NoSuchElementException.class)
    public void shouldThrowWhenNoSuchElement() throws Exception {
        CommandProcessorMetrics sut = new CommandProcessorMetrics(Collections.emptyList(), 100L);
        sut.getCommand(0);
    }

    @Test
    public void shouldPrintToString() throws Exception {
        CommandProcessorMetrics sut = new CommandProcessorMetrics(Collections.emptyList(), 100L);
        assertTrue(sut.toString().length() > 10);
    }
}