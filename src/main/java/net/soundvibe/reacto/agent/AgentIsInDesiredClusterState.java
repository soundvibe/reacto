package net.soundvibe.reacto.agent;

public class AgentIsInDesiredClusterState extends RuntimeException {

    public AgentIsInDesiredClusterState(String name, int desiredNoOfInstances) {
        super(String.format("There are already desired number [%s] of agent's [%s] instances running in the cluster",
                desiredNoOfInstances, name));
    }
}
