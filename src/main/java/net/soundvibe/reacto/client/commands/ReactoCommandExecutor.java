package net.soundvibe.reacto.client.commands;

import net.soundvibe.reacto.client.events.CommandHandler;
import net.soundvibe.reacto.discovery.*;
import net.soundvibe.reacto.errors.CannotFindEventHandlers;
import net.soundvibe.reacto.types.*;
import rx.Observable;

import java.util.*;

/**
 * @author OZY on 2016.09.06.
 */
public final class ReactoCommandExecutor implements CommandExecutor {

    private final List<CommandHandler> commandHandlers;
    private final LoadBalancer<CommandHandler> loadBalancer;

    public static final CommandExecutorFactory FACTORY = ReactoCommandExecutor::new;

    public ReactoCommandExecutor(List<CommandHandler> commandHandlers,
                                 LoadBalancer<CommandHandler> loadBalancer) {
        Objects.requireNonNull(commandHandlers, "commandHandlers cannot be null");
        Objects.requireNonNull(loadBalancer, "loadBalancer cannot be null");
        this.commandHandlers = commandHandlers;
        this.loadBalancer = loadBalancer;
    }

    @Override
    public Observable<Event> execute(Command command) {
        if (commandHandlers.isEmpty()) return Observable.error(new CannotFindEventHandlers("No event handlers found for command: " + command));
        return Observable.just(commandHandlers)
                .map(loadBalancer::balance)
                .concatMap(eventHandler -> eventHandler.observe(command)
                        .onBackpressureBuffer()
                        .onErrorResumeNext(error -> handleError(error, command, eventHandler)))
                ;
    }

    private Observable<Event> handleError(Throwable error, Command command, CommandHandler commandHandler) {
        return Observable.just(commandHandler)
                .doOnNext(this::removeHandler)
                .flatMap(any -> commandHandlers.isEmpty() ?  Observable.error(error) : Observable.just(command))
                .flatMap(this::execute);
    }

    private synchronized void removeHandler(CommandHandler commandHandler) {
        commandHandlers.remove(commandHandler);
    }
}
