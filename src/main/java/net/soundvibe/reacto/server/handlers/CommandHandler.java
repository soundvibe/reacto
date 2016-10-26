package net.soundvibe.reacto.server.handlers;

import io.vertx.core.logging.*;
import net.soundvibe.reacto.client.errors.CommandNotFound;
import net.soundvibe.reacto.internal.InternalEvent;
import net.soundvibe.reacto.mappers.Mappers;
import net.soundvibe.reacto.server.CommandRegistry;
import net.soundvibe.reacto.types.*;
import rx.*;
import rx.schedulers.Schedulers;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.function.*;

/**
 * @author OZY on 2016.02.09.
 */
public final class CommandHandler {

    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    private final CommandRegistry commands;

    private static final Scheduler SINGLE_THREAD = Schedulers.from(Executors.newSingleThreadExecutor());

    public CommandHandler(CommandRegistry commands) {
        this.commands = commands;
    }

    public void handle(final byte[] bytes,
                       Consumer<byte[]> sender,
                       Consumer<Subscription> unSubscriber,
                       Runnable closeHandler
                       ) {
        try {
            final Command receivedCommand = Mappers.fromBytesToCommand(bytes);
            final Optional<Function<Command, Observable<Event>>> commandFunc = commands.findCommand(receivedCommand.name);
            commandFunc
                    .map(cmdFunc -> cmdFunc.apply(receivedCommand)
                            .doOnEach(notification -> log.debug("Command "+ receivedCommand + " executed and received notification: " + notification))
                            .subscribeOn(SINGLE_THREAD)
                            .subscribe(
                                    event -> sender.accept(toBytes(InternalEvent.onNext(event))),
                                    throwable -> {
                                        sender.accept(toBytes(InternalEvent.onError(throwable)));
                                        closeHandler.run();
                                    },
                                    () -> {
                                        sender.accept(toBytes(InternalEvent.onCompleted()));
                                        closeHandler.run();
                                    }))
                    .ifPresent(unSubscriber);

            if (!commandFunc.isPresent()) {
                sender.accept(toBytes(InternalEvent.onError(new CommandNotFound(receivedCommand.name))));
                closeHandler.run();
            }
        } catch (Throwable e) {
            sender.accept(toBytes(InternalEvent.onError(e)));
            closeHandler.run();
        }
    }

    private byte[] toBytes(InternalEvent internalEvent) {
        return Mappers.internalEventToBytes(internalEvent);
    }


}
