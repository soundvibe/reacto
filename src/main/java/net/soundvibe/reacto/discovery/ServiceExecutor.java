package net.soundvibe.reacto.discovery;

import io.reactivex.Flowable;
import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.events.CommandHandler;
import net.soundvibe.reacto.types.*;

/**
 * @author OZY on 2017.02.09.
 */
public interface ServiceExecutor {

    default Flowable<Event> execute(Command command) {
        return execute(command, Event.class);
    }

    default <E,C> Flowable<E> execute(C command, Class<? extends E> eventClass) {
        return execute(command, eventClass, LoadBalancers.ROUND_ROBIN, ReactoCommandExecutor.FACTORY);
    }

    default <E,C> Flowable<E> execute(C command, Class<? extends E> eventClass, CommandExecutorFactory commandExecutorFactory) {
        return execute(command, eventClass, LoadBalancers.ROUND_ROBIN, commandExecutorFactory);
    }

    <E,C> Flowable<E> execute(
            C command,
            Class<? extends E> eventClass,
            LoadBalancer<CommandHandler> loadBalancer,
            CommandExecutorFactory commandExecutorFactory);

}
