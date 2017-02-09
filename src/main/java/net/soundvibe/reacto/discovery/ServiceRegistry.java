package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.client.commands.*;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.types.*;
import rx.Observable;

/**
 * @author linas on 17.1.9.
 */
public interface ServiceRegistry {

    default Observable<Event> execute(Command command) {
        return execute(command, Event.class);
    }

    default <E,C> Observable<E> execute(C command, Class<? extends E> eventClass) {
        return execute(command, eventClass, LoadBalancers.ROUND_ROBIN, ReactoCommandExecutor.FACTORY);
    }

    default <E,C> Observable<E> execute(C command, Class<? extends E> eventClass, CommandExecutorFactory commandExecutorFactory) {
        return execute(command, eventClass, LoadBalancers.ROUND_ROBIN, commandExecutorFactory);
    }

    <E,C> Observable<E> execute(
            C command,
            Class<? extends E> eventClass,
            LoadBalancer<EventHandler> loadBalancer,
            CommandExecutorFactory commandExecutorFactory);

}
