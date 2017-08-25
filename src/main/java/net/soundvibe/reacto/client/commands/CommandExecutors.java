package net.soundvibe.reacto.client.commands;

/**
 * @author Cipolinas on 2015.12.01.
 */
public interface CommandExecutors {

    static CommandExecutorFactory reacto() {
        return ReactoCommandExecutor.FACTORY;
    }
}
