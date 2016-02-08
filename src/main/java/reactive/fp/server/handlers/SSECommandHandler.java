package reactive.fp.server.handlers;

import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import reactive.fp.server.CommandRegistry;
import reactive.fp.types.Command;
import reactive.fp.internal.InternalEvent;
import rx.Subscription;
import rx.schedulers.Schedulers;

import static reactive.fp.mappers.Mappers.fromBytesToCommand;

/**
 * @author Linas on 2015.12.03.
 */
public class SSECommandHandler implements Handler<RoutingContext> {

    private final SSEHandler sseHandler;
    private final CommandRegistry commands;

    public SSECommandHandler(SSEHandler sseHandler, CommandRegistry commands) {
        this.sseHandler = sseHandler;
        this.commands = commands;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        commands.findCommand(getCommandNameFrom(routingContext.request())).ifPresent(command -> routingContext.request().bodyHandler(buffer -> {
            try {
                Command receivedArgument = fromBytesToCommand(buffer.getBytes());
                final Subscription subscription = command.apply(receivedArgument)
                        .subscribeOn(Schedulers.computation())
                        .subscribe(
                                event -> sseHandler.writeEvent(InternalEvent.onNext(event)),
                                throwable -> sseHandler.writeEvent(InternalEvent.onError(throwable)),
                                () -> sseHandler.writeEvent(InternalEvent.onCompleted()));
            } catch (Throwable e) {
                sseHandler.writeEvent(InternalEvent.onError(e));
            }
        }));
        routingContext.response().end();
    }

    private String getCommandNameFrom(HttpServerRequest request) {
        return request.getHeader("command");
    }

}
