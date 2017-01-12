package net.soundvibe.reacto.metric;

import rx.Observable;
import rx.subjects.*;

import java.util.concurrent.TimeUnit;

/**
 * @author OZY on 2017.01.12.
 */
public final class ReactoDashboardStream {

    public static final long DELAY_IN_MS = 500L;

    private static final Subject<CommandHandlerMetric, CommandHandlerMetric> commandHandlerSubject =
            new SerializedSubject<>(PublishSubject.create());

    private static final Subject<EventHandlerMetrics, EventHandlerMetrics> eventHandlerSubject =
            new SerializedSubject<>(PublishSubject.create());


    /**
     * Observes metrics, collected from server side Command Handlers
     * @return CommandHandlerMetrics observable. CommandHandlerMetrics is an aggregated entity.
     * Observable will emit this event in DELAY_IN_MS intervals (500 ms).
     * Subscribers should unsubscribe from this stream when it is not needed because stream will never complete.
     */
    public static Observable<CommandHandlerMetrics> observeCommandHandlers() {
        return observeCommandHandlers(DELAY_IN_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Observes metrics, collected from server side Command Handlers
     * @return CommandHandlerMetrics observable. CommandHandlerMetrics is an aggregated entity.
     * Observable will emit this event in DELAY_IN_MS intervals (500 ms).
     * Subscribers should unsubscribe from this stream when it is not needed because stream will never complete.
     */
    public static Observable<CommandHandlerMetrics> observeCommandHandlers(long delay, TimeUnit timeUnit) {
        return commandHandlerSubject.buffer(delay, timeUnit)
                .map(CommandHandlerMetrics::new)
                .share()
                .onBackpressureDrop();
    }

    static void publishCommandHandlerMetric(CommandHandlerMetric commandHandlerMetric) {
        commandHandlerSubject.onNext(commandHandlerMetric);
    }
}
