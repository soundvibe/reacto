package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.client.commands.CommandExecutor;
import net.soundvibe.reacto.client.events.EventHandler;
import net.soundvibe.reacto.types.*;
import rx.Observable;

/**
 * @author linas on 17.1.9.
 */
public interface ServiceRegistry {

    default Observable<CommandExecutor> find(String commandName) {
        return find(commandName, LoadBalancers.ROUND_ROBIN);
    }

    Observable<CommandExecutor> find(String commandName, LoadBalancer<EventHandler> loadBalancer);

    default Observable<Event> execute(Command command) {
        return execute(command, LoadBalancers.ROUND_ROBIN);
    }

    Observable<Event> execute(Command command, LoadBalancer<EventHandler> loadBalancer);

}
