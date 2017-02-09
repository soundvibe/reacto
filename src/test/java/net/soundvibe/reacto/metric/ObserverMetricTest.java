package net.soundvibe.reacto.metric;

import com.codahale.metrics.*;
import net.soundvibe.reacto.types.CommandDescriptor;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

/**
 * @author linas on 17.2.9.
 */
public class ObserverMetricTest {

    @Test
    public void shouldCalculateMetrics() throws Exception {
        final ConsoleReporter reporter = ConsoleReporter.forRegistry(Metrics.REGISTRY)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build();

        CommandDescriptor descriptor = CommandDescriptor.of("test-metrics");

        TestSubscriber<Integer> testSubscriber = new TestSubscriber<>();
        Observable.range(1, 100)
                .subscribeOn(Schedulers.computation())
                .doOnNext(integer -> sleep(1L))
                .flatMap(i -> i > 98 ? Observable.error(new RuntimeException("error"))
                        : Observable.just(i))
                .doOnEach(ObserverMetric.findObserver(descriptor))
                .subscribe(testSubscriber);

        testSubscriber.awaitTerminalEvent();

        reporter.report();

        SortedMap<String, Meter> meters = Metrics.REGISTRY.getMeters();

        Meter errorMeter = meters.get(ObserverMetric.getName(descriptor, ObserverMetric.NAME_METER_ON_ERROR));
        assertEquals(1L, errorMeter.getCount());

        Meter onNextMeter = meters.get(ObserverMetric.getName(descriptor, ObserverMetric.NAME_METER_ON_NEXT));
        assertEquals(98, onNextMeter.getCount());
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //
        }
    }
}