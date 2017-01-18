package net.soundvibe.reacto.server.vertx.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import net.soundvibe.reacto.discovery.ServiceDiscoveryLifecycle;
import net.soundvibe.reacto.discovery.types.ServiceRecord;
import net.soundvibe.reacto.server.CommandRegistry;
import rx.Observable;

import java.util.function.Supplier;

import static net.soundvibe.reacto.server.vertx.VertxServer.INTERNAL_SERVER_ERROR;

/**
 * @author OZY on 2016.08.28.
 */
public class ServiceDiscoveryHandler implements Handler<RoutingContext> {

    private final ServiceDiscoveryLifecycle controller;
    private final Supplier<ServiceRecord> record;
    private final CommandRegistry commandRegistry;

    public ServiceDiscoveryHandler(ServiceDiscoveryLifecycle controller, Supplier<ServiceRecord> record, CommandRegistry commandRegistry) {
        this.controller = controller;
        this.record = record;
        this.commandRegistry = commandRegistry;
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
                        .flatMap(ctrl -> ctrl.startDiscovery(record.get(), commandRegistry))
                        .subscribe(__ -> ctx.response().end(new JsonObject()
                                .put("message", "Service discovery was started successfully")
                                .encode())
                                , throwable -> ctx.response()
                                        .setStatusCode(INTERNAL_SERVER_ERROR)
                                        .setStatusMessage(throwable.getClass().getSimpleName())
                                        .end(throwable.toString()));
                break;
            }

            case "close": {
                Observable.just(controller)
                        .filter(ctrl -> record.get() != null)
                        .flatMap(ServiceDiscoveryLifecycle::closeDiscovery)
                        .subscribe(__ -> ctx.response().end(new JsonObject()
                                        .put("message", "Service discovery was closed successfully")
                                        .encode())
                                , throwable -> ctx.response()
                                        .setStatusCode(INTERNAL_SERVER_ERROR)
                                        .setStatusMessage(throwable.getClass().getSimpleName())
                                        .end(throwable.toString()));
                break;
            }
        }
    }
}
