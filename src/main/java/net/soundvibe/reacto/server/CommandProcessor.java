package net.soundvibe.reacto.server;

import net.soundvibe.reacto.errors.CommandNotFound;
import net.soundvibe.reacto.mappers.Mappers;
import net.soundvibe.reacto.types.*;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * @author Linas on 2017.01.12.
 */
public class CommandProcessor {

    private final CommandRegistry commands;

    public CommandProcessor(CommandRegistry commands) {
        this.commands = commands;
    }

    public Observable<Event> process(byte[] bytes) {
        return Observable.just(bytes)
                .map(Mappers::fromBytesToCommand)
                .flatMap(this::process);
    }

    public Observable<Event> process(Command command) {
        return Observable.just(command)
                .concatMap(cmd -> commands.findCommand(CommandDescriptor.fromCommand(cmd))
                        .map(commandExecutor -> commandExecutor.execute(cmd))
                        .orElseGet(() -> Observable.error(new CommandNotFound(cmd.name))))
                .subscribeOn(Schedulers.computation());
    }
}
