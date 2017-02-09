package net.soundvibe.reacto.metric;

import com.codahale.metrics.*;
import net.soundvibe.reacto.types.*;
import rx.Observer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author linas on 17.2.9.
 */
public final class ObserverMetric<T> implements Observer<T> {

    private final static Map<CommandDescriptor, ObserverMetric> observers = new ConcurrentHashMap<>();
    public static final String NAME_METER_ON_NEXT = "Meter:OnNext";
    public static final String NAME_METER_ON_ERROR = "Meter:onError";
    public static final String NAME_HISTOGRAM_ON_NEXT = "Histogram:OnNext";
    private final Histogram histogram;
    private final Meter onNextMeter;
    private final Meter errorMeter;
    private final AtomicInteger onNextCount = new AtomicInteger(0);

    @SuppressWarnings("unchecked")
    public static <T> ObserverMetric<T> findObserver(CommandDescriptor commandDescriptor) {
        return observers.computeIfAbsent(commandDescriptor, ObserverMetric::new);
    }

    private ObserverMetric(CommandDescriptor commandDescriptor) {
        this.histogram = Metrics.REGISTRY.histogram(
                getName(commandDescriptor, NAME_HISTOGRAM_ON_NEXT));
        this.onNextMeter = Metrics.REGISTRY.meter(
                getName(commandDescriptor, NAME_METER_ON_NEXT));
        this.errorMeter = Metrics.REGISTRY.meter(
                getName(commandDescriptor, NAME_METER_ON_ERROR));
    }

    public static String getName(CommandDescriptor descriptor, String name) {
        return descriptor.eventType.isEmpty() ?
                descriptor.commandType + ":" + name :
                descriptor.commandType + ":" + descriptor.eventType + ":" + name;
    }

    @Override
    public void onCompleted() {
        histogram.update(onNextCount.get());
        onNextCount.set(0);
    }

    @Override
    public void onError(Throwable throwable) {
        errorMeter.mark();
        histogram.update(onNextCount.get());
        onNextCount.set(0);
    }

    @Override
    public void onNext(T o) {
        onNextCount.incrementAndGet();
        onNextMeter.mark();
    }
}
