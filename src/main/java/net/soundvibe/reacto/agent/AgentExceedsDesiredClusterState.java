package net.soundvibe.reacto.agent;

public class AgentExceedsDesiredClusterState extends RuntimeException {

    public AgentExceedsDesiredClusterState(String name, int desiredNoOfInstances) {
        super(String.format("There are already more than desired number [%s] of agent's [%s] instances running in the cluster",
                desiredNoOfInstances, name));
    }

}
