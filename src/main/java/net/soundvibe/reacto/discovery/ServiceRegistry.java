package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.client.events.EventHandler;
import rx.Observable;

/**
 * @author linas on 17.1.9.
 */
public interface ServiceRegistry {

    default <E,C> Observable<? extends E> execute(C command, Class<? extends E> eventClass) {
        return execute(command, eventClass, LoadBalancers.ROUND_ROBIN);
    }

    <E,C> Observable<? extends E> execute(C command, Class<? extends E> eventClass, LoadBalancer<EventHandler> loadBalancer);

}
