package net.soundvibe.reacto.server.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.Status;
import net.soundvibe.reacto.discovery.DiscoverableService;
import net.soundvibe.reacto.utils.Factories;
import rx.Observable;


import java.util.function.Supplier;

import static net.soundvibe.reacto.server.VertxServer.INTERNAL_SERVER_ERROR;

/**
 * @author OZY on 2016.08.28.
 */
public class ServiceDiscoveryHandler implements Handler<RoutingContext> {

    private final DiscoverableService serviceDiscovery;
    private final Supplier<Record> record;

    public ServiceDiscoveryHandler(DiscoverableService serviceDiscovery, Supplier<Record> record) {
        this.serviceDiscovery = serviceDiscovery;
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
                Observable.just(serviceDiscovery)
                        .filter(discovery -> record.get() != null)
                        .flatMap(discovery -> discovery.startDiscovery(record.get()))
                        .subscribe(rec -> ctx.response().end(rec.toJson().toString())
                                , throwable -> ctx.response()
                                        .setStatusCode(INTERNAL_SERVER_ERROR)
                                        .setStatusMessage(throwable.toString())
                                        .end());
                break;
            }

            case "close": {
                Observable.just(serviceDiscovery)
                        .filter(discovery -> record.get() != null)
                        .flatMap(discovery -> discovery.closeDiscovery(record.get()))
                        .subscribe(record -> ctx.response().end(record.toJson().toString())
                                , throwable -> ctx.response()
                                        .setStatusCode(INTERNAL_SERVER_ERROR)
                                        .setStatusMessage(throwable.toString())
                                        .end());
                break;
            }
            case "clean": {
                Observable.just(serviceDiscovery)
                        .subscribeOn(Factories.SINGLE_THREAD)
                        .flatMap(discovery -> discovery.removeRecordsWithStatus(Status.DOWN))
                        .subscribe(record -> ctx.response().write(record.toJson().toString())
                                , throwable -> ctx.response()
                                        .setStatusCode(INTERNAL_SERVER_ERROR)
                                        .setStatusMessage(throwable.toString())
                                        .end(),
                                () -> ctx.response().end());
                break;
            }
        }
    }
}
