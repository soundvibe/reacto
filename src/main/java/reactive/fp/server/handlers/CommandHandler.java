package reactive.fp.server.handlers;

import reactive.fp.client.errors.CommandNotFound;
import reactive.fp.internal.InternalEvent;
import reactive.fp.mappers.Mappers;
import reactive.fp.server.CommandRegistry;
import reactive.fp.types.Command;
import reactive.fp.types.Event;
import rx.Observable;
import rx.Subscription;
import rx.schedulers.Schedulers;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static reactive.fp.mappers.Mappers.fromBytesToCommand;

/**
 * @author OZY on 2016.02.09.
 */
public final class CommandHandler {

    private final CommandRegistry commands;

    public CommandHandler(CommandRegistry commands) {
        this.commands = commands;
    }

    public void handle(final byte[] bytes,
                          Consumer<byte[]> sender,
                          Consumer<Subscription> unSubscriber) {
        try {
            final Command receivedCommand = fromBytesToCommand(bytes);
            final Optional<Function<Command, Observable<Event>>> commandFunc = commands.findCommand(receivedCommand.name);
            commandFunc
                    .map(cmdFunc -> cmdFunc.apply(receivedCommand)
                            .subscribeOn(Schedulers.computation())
                            .subscribe(
                                    event -> sender.accept(toBytes(InternalEvent.onNext(event))),
                                    throwable -> sender.accept(toBytes(InternalEvent.onError(throwable))),
                                    () -> sender.accept(toBytes(InternalEvent.onCompleted()))))
                    .ifPresent(unSubscriber::accept);

            if (!commandFunc.isPresent()) {
                sender.accept(toBytes(InternalEvent.onError(new CommandNotFound(receivedCommand.name))));
            }
        } catch (Throwable e) {
            sender.accept(toBytes(InternalEvent.onError(e)));
        }
    }

    private byte[] toBytes(InternalEvent internalEvent) {
        return Mappers.internalEventToBytes(internalEvent);
    }


}
