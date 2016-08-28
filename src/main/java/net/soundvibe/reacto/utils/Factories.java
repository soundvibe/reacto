package net.soundvibe.reacto.utils;

import net.soundvibe.reacto.internal.Lazy;
import io.vertx.core.Vertx;
import rx.Scheduler;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executors;

/**
 * @author OZY on 2015.11.24.
 */
public final class Factories {

    private Factories() {
        //
    }

    private static final Lazy<Vertx> vertxLazy = Lazy.of(Vertx::vertx);

    public static Vertx vertx() {
        return vertxLazy.get();
    }

    public static final Scheduler COMPUTATION = Schedulers.computation();
    public static final Scheduler SINGLE_THREAD = Schedulers.from(Executors.newSingleThreadExecutor());

}
