package net.soundvibe.reacto.agent;

import org.reactivestreams.Publisher;

/**
 * Represents worker Agent for running any given tasks.
 * Agents should be run from {@link AgentSystem}. AgentSystem will be responsible for supervising given Agent and apply
 * @param <T>
 */
public interface Agent<T> {

    default String name() {
        return getClass().getSimpleName();
    }

    Publisher<T> run();

    AgentOptions options();

    int version();

    default void close() {
        //implement if need to close resources
    }
}
