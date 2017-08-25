package net.soundvibe.reacto.client.commands;

import net.soundvibe.reacto.client.events.CommandHandler;
import net.soundvibe.reacto.discovery.*;

import java.util.List;

/**
 * @author Linas on 2017.01.18.
 */
@FunctionalInterface
public interface CommandExecutorFactory {

    CommandExecutor create(List<CommandHandler> commandHandlers,
                           LoadBalancer<CommandHandler> loadBalancer);

}
