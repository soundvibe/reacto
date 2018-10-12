package net.soundvibe.reacto.agent;

import org.reactivestreams.Publisher;

/**
 * Represents worker Agent for running any given tasks.
 * Agents should be run from {@link AgentSystem}. AgentSystem will be responsible for supervising given Agent.
 * @param <T> Type of events agent will be processing
 */
public interface Agent<T> {

    /**
     * @return Name of the agent
     */
    default String name() {
        return getClass().getSimpleName();
    }

    /**
     * Main agent logic should be implemented here
     * @return Reactive streams compliant publisher of agent events
     */
    Publisher<T> run();

    /**
     * Represents agent configuration options
     * @return AgentOptions
     */
    AgentOptions options();

    /**
     * Agents should explicitly define their version number so that upgrades could be handled more easily.
     * @return version number
     */
    int version();

    /**
     * Implement if agent needs to close resources
     */
    default void close() {
        //implement if need to close resources
    }
}
