package net.soundvibe.reacto.agent;

public interface AgentFactory<T extends Agent<?>> {

    T create();

}
