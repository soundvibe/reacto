package reactive.fp.utils;

import io.vertx.core.Vertx;

/**
 * @author OZY on 2015.11.24.
 */
public final class Factories {

    private Factories() {
        //
    }

    private static final Vertx vertx = Vertx.vertx();

    public static Vertx vertx() {
        return vertx;
    }

}
