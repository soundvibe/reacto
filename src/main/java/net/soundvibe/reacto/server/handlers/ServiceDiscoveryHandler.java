package net.soundvibe.reacto.server.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.Record;
import net.soundvibe.reacto.discovery.*;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;

import java.util.function.Supplier;

import static net.soundvibe.reacto.server.VertxServer.INTERNAL_SERVER_ERROR;

/**
 * @author OZY on 2016.08.28.
 */
public class ServiceDiscoveryHandler implements Handler<RoutingContext> {

    private final ServiceDiscoveryLifecycle controller;
    private final Supplier<Record> record;

    public ServiceDiscoveryHandler(ServiceDiscoveryLifecycle controller, Supplier<Record> record) {
        this.controller = controller;
        this.record = record;
    }

    @Override
    public void handle(RoutingContext ctx) {
        final String action = ctx.request().getParam("action");
        if (action == null) {
            ctx.response().setStatusCode(404).setStatusMessage("Action not found").end();
            return;
        }

        switch (action) {
            case "start" : {
                Observable.just(controller)
                        .filter(ctrl -> record.get() != null)
                        .flatMap(ctrl -> ctrl.startDiscovery(record.get()))
                        .subscribe(rec -> ctx.response().end(rec.toJson().toString())
                                , throwable -> ctx.response()
                                        .setStatusCode(INTERNAL_SERVER_ERROR)
                                        .setStatusMessage(throwable.getClass().getSimpleName())
                                        .end(throwable.toString()));
                break;
            }

            case "close": {
                Observable.just(controller)
                        .filter(ctrl -> record.get() != null)
                        .flatMap(ctrl -> ctrl.closeDiscovery(record.get()))
                        .subscribe(record -> ctx.response().end(record.toJson().toString())
                                , throwable -> ctx.response()
                                        .setStatusCode(INTERNAL_SERVER_ERROR)
                                        .setStatusMessage(throwable.getClass().getSimpleName())
                                        .end(throwable.toString()));
                break;
            }
            case "clean": {
                Observable.just(controller)
                        .subscribeOn(Factories.SINGLE_THREAD)
                        .flatMap(ServiceDiscoveryLifecycle::cleanServices)
                        .subscribe(record -> ctx.response().write(record.toJson().toString())
                                , throwable -> ctx.response()
                                        .setStatusCode(INTERNAL_SERVER_ERROR)
                                        .setStatusMessage(throwable.getClass().getSimpleName())
                                        .end(throwable.toString()),
                                () -> ctx.response().end());
                break;
            }
        }
    }
}
