package net.soundvibe.reacto.server;

import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.errors.CommandNotFound;
import net.soundvibe.reacto.mappers.Mappers;
import net.soundvibe.reacto.types.*;
import rx.*;
import rx.schedulers.Schedulers;

import java.util.concurrent.Executors;

/**
 * @author Linas on 2017.01.12.
 */
public class CommandProcessor implements CommandExecutor {

    private static final Scheduler SINGLE_THREAD = Schedulers.from(Executors.newSingleThreadExecutor());

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
                .subscribeOn(SINGLE_THREAD);
    }

    @Override
    public Observable<Event> execute(Command command) {
        return process(command);
    }
}
