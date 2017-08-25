package net.soundvibe.reacto.metric;

import com.codahale.metrics.*;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.TestSubscriber;
import net.soundvibe.reacto.types.*;
import org.junit.Test;

import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

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
        Flowable.range(1, 100)
                .subscribeOn(Schedulers.computation())
                .doOnNext(integer -> sleep(1L))
                .flatMap(i -> i > 98 ? Flowable.error(new RuntimeException("error"))
                        : Flowable.just(i))
                .doOnEach(ObserverMetric.findObserver(Command.create("test-metrics")))
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