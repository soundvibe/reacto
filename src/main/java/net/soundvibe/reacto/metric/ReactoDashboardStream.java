package net.soundvibe.reacto.metric;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.*;

import java.util.concurrent.TimeUnit;

/**
 * @author OZY on 2017.01.12.
 */
public final class ReactoDashboardStream {

    public static final long DELAY_IN_MS = 500L;

    private static final Subject<CommandHandlerMetric, CommandHandlerMetric> commandHandlerSubject =
            new SerializedSubject<>(PublishSubject.create());

    private static final Subject<EventHandlerMetrics, EventHandlerMetrics> eventHandlerMetrics =
            new SerializedSubject<>(PublishSubject.create());


    public static Observable<CommandHandlerMetrics> observeCommandHandlers() {
        return commandHandlerSubject.buffer(DELAY_IN_MS, TimeUnit.MILLISECONDS, Schedulers.computation())
                .map(CommandHandlerMetrics::new)
                .share()
                .onBackpressureDrop();
    }

    static void publishCommandHandlerMetric(CommandHandlerMetric commandHandlerMetric) {
        commandHandlerSubject.onNext(commandHandlerMetric);
    }
}
