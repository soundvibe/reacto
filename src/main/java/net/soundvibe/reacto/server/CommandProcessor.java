package net.soundvibe.reacto.server;

import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import net.soundvibe.reacto.errors.CommandNotFound;
import net.soundvibe.reacto.mappers.Mappers;
import net.soundvibe.reacto.types.*;

/**
 * @author Linas on 2017.01.12.
 */
public class CommandProcessor {

    private final CommandRegistry commands;

    public CommandProcessor(CommandRegistry commands) {
        this.commands = commands;
    }

    public Flowable<Event> process(byte[] bytes) {
        return Flowable.just(bytes)
                .map(Mappers::fromBytesToCommand)
                .flatMap(this::process);
    }

    public Flowable<Event> process(Command command) {
        return Flowable.just(command)
                .concatMap(cmd -> commands.findCommand(CommandDescriptor.fromCommand(cmd))
                        .map(commandExecutor -> commandExecutor.execute(cmd))
                        .orElseGet(() -> Flowable.error(new CommandNotFound(cmd.name))))
                .subscribeOn(Schedulers.computation());
    }
}
