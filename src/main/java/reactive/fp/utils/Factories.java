package reactive.fp.utils;

import io.vertx.core.Vertx;
import reactive.fp.internal.Lazy;

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

}
