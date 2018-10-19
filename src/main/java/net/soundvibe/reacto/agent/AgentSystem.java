package net.soundvibe.reacto.agent;

import io.reactivex.*;

import java.io.Closeable;

public interface AgentSystem<T extends AgentFactory<?>> extends Closeable {

    Maybe<String> run(T agentFactory);
}
