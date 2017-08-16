package net.soundvibe.reacto.client.commands;

import io.reactivex.Flowable;
import io.reactivex.functions.Function;
import net.soundvibe.reacto.client.events.CommandHandler;
import net.soundvibe.reacto.discovery.LoadBalancer;
import net.soundvibe.reacto.errors.CannotFindEventHandlers;
import net.soundvibe.reacto.types.*;
import org.reactivestreams.Publisher;

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
    public Flowable<Event> execute(Command command) {
        if (commandHandlers.isEmpty()) return Flowable.error(new CannotFindEventHandlers("No command handlers found for command: " + command));
        return Flowable.just(commandHandlers)
                .map(loadBalancer::balance)
                .concatMap(eventHandler -> eventHandler.observe(command)
                        .onErrorResumeNext((Function<? super Throwable, ? extends Publisher<? extends Event>>) error -> handleError(error, command, eventHandler)))
                ;
    }

    private Flowable<Event> handleError(Throwable error, Command command, CommandHandler commandHandler) {
        return Flowable.just(commandHandler)
                .doOnNext(this::removeHandler)
                .flatMap(any -> commandHandlers.isEmpty() ?  Flowable.error(error) : Flowable.just(command))
                .flatMap(this::execute);
    }

    private synchronized void removeHandler(CommandHandler commandHandler) {
        commandHandlers.remove(commandHandler);
    }
}
