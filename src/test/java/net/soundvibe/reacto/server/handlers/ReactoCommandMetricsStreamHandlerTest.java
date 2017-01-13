package net.soundvibe.reacto.server.handlers;

import net.soundvibe.reacto.metric.*;
import net.soundvibe.reacto.types.Command;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertTrue;

/**
 * @author Linas on 2017.01.12.
 */
public class ReactoCommandMetricsStreamHandlerTest {


    @Test
    public void shouldGetJson() throws Exception {
        final CommandProcessorMetrics metrics = new CommandProcessorMetrics(Arrays.asList(
                CommandProcessorMetric.of(Command.create("demo")).onNext().onNext().onCompleted(),
                CommandProcessorMetric.of(Command.create("foo")).onNext().onNext().onCompleted()
        ), ReactoDashboardStream.DELAY_IN_MS);
        final String json = ReactoCommandMetricsStreamHandler.getJson(metrics);
        System.out.println(json);
        assertTrue(json.startsWith("{"));
        assertTrue(json.endsWith("}"));
        assertTrue(json.length() > 10);
    }
}