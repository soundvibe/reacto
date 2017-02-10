package net.soundvibe.reacto.metric;

import com.codahale.metrics.*;
import net.soundvibe.reacto.types.*;
import rx.Observer;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author linas on 17.2.9.
 */
public final class ObserverMetric<T> implements Observer<T> {

    private final static Map<CommandDescriptor, ObserverMetric> observers = new ConcurrentHashMap<>();
    public static final String NAME_METER_ON_NEXT = "Meter:Events";
    public static final String NAME_METER_ON_ERROR = "Meter:Errors";
    public static final String NAME_TIMER_COMMAND = "Timer:Commands";
    private final Meter onNextMeter;
    private final Meter errorMeter;
    private final Timer timer;

    @SuppressWarnings("unchecked")
    public static <T> ObserverMetric<T> findObserver(Command command) {
        return observers.computeIfAbsent(CommandDescriptor.fromCommand(command), ObserverMetric::new);
    }

    private ObserverMetric(CommandDescriptor commandDescriptor) {
        this.onNextMeter = Metrics.REGISTRY.meter(
                getName(commandDescriptor, NAME_METER_ON_NEXT));
        this.errorMeter = Metrics.REGISTRY.meter(
                getName(commandDescriptor, NAME_METER_ON_ERROR));
        this.timer = Metrics.REGISTRY.timer(getName(commandDescriptor, NAME_TIMER_COMMAND));
    }

    public static String getName(CommandDescriptor descriptor, String name) {
        return descriptor.eventType.isEmpty() ?
                descriptor.commandType + ":" + name :
                descriptor.commandType + ":" + descriptor.eventType + ":" + name;
    }

    public Pair<ObserverMetric<T>, Timer.Context> startTimer() {
        return Pair.of(this, timer.time());
    }

    @Override
    public void onCompleted() {
        //do nothing
    }

    @Override
    public void onError(Throwable throwable) {
        errorMeter.mark();
    }

    @Override
    public void onNext(T o) {
        onNextMeter.mark();
    }
}
