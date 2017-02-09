package net.soundvibe.reacto.metric;

import rx.Observable;
import rx.subjects.*;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author OZY on 2017.01.12.
 */
public final class ReactoDashboardStream {

    private ReactoDashboardStream() {
        //no-instance
    }

    public static final long DELAY_IN_MS = 500L;

    private static final Subject<CommandProcessorMetric, CommandProcessorMetric> commandProcessorSubject =
            new SerializedSubject<>(PublishSubject.create());


    /**
     * Observes metrics, collected from server side Command Handlers
     * @return CommandProcessorMetrics observable. CommandProcessorMetrics is an aggregated entity.
     * Observable will emit this event in DELAY_IN_MS intervals (500 ms).
     * Subscribers should unsubscribe from this stream when it is not needed because stream will never complete.
     */
    public static Observable<CommandProcessorMetrics> observeCommandHandlers() {
        return observeCommandHandlers(DELAY_IN_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Observes metrics, collected from server side Command Handlers
     * @return CommandProcessorMetrics observable. CommandProcessorMetrics is an aggregated entity.
     * Observable will emit this event in DELAY_IN_MS intervals (500 ms).
     * Subscribers should unsubscribe from this stream when it is not needed because stream will never complete.
     */
    public static Observable<CommandProcessorMetrics> observeCommandHandlers(long delay, TimeUnit timeUnit) {
        return commandProcessorSubject.buffer(delay, timeUnit)
                .flatMap(ReactoDashboardStream::aggregate)
                .map(elements -> new CommandProcessorMetrics(elements, timeUnit.toMillis(delay)))
                .share()
                .onBackpressureDrop();
    }

    private static Observable<Collection<CommandProcessorMetric>> aggregate(List<CommandProcessorMetric> metrics) {
        if (metrics.isEmpty()) return Observable.just(metrics);
        final Map<String, CommandProcessorMetric> grouped = metrics.stream()
                .collect(Collectors.toMap(
                        ReactoDashboardStream::getKey,
                        Function.identity(),
                        CommandProcessorMetric::accumulate));
        return Observable.just(grouped.values());
    }

    private static String getKey(CommandProcessorMetric commandProcessorMetric) {
        return commandProcessorMetric.commandName() + ":" + commandProcessorMetric.eventName();
    }

    static void publishCommandHandlerMetric(CommandProcessorMetric commandProcessorMetric) {
        commandProcessorSubject.onNext(commandProcessorMetric);
    }
}
