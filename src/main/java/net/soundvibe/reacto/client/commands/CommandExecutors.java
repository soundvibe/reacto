package net.soundvibe.reacto.client.commands;

import com.netflix.hystrix.HystrixCommandProperties;
import net.soundvibe.reacto.client.commands.hystrix.HystrixCommandExecutor;

/**
 * @author Cipolinas on 2015.12.01.
 */
public interface CommandExecutors {

    static CommandExecutorFactory reacto() {
        return ReactoCommandExecutor.FACTORY;
    }

    static CommandExecutorFactory hystrix() {
        return HystrixCommandExecutor.FACTORY;
    }

    static CommandExecutorFactory hystrix(HystrixCommandProperties.Setter setter) {
        return (eventHandlers, loadBalancer, serviceRegistry) -> new HystrixCommandExecutor(eventHandlers, setter);
    }
}
