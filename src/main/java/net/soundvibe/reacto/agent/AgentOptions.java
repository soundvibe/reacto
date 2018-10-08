package net.soundvibe.reacto.agent;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.Objects;

public abstract class AgentOptions {

    private int clusterInstances = 1;
    private int maxInstancesOnNode = 4;
    private boolean isHA = false;
    private boolean undeployOnComplete = true;
    private AgentRestartStrategy agentRestartStrategy = AlwaysRestart.INSTANCE;

    public boolean isHA() {
        return isHA;
    }

    public AgentOptions setHA(boolean value) {
        this.isHA = value;
        return this;
    }

    public int getClusterInstances() {
        return clusterInstances;
    }

    public AgentOptions setClusterInstances(int clusterInstances) {
        if (clusterInstances < 1) throw new IllegalArgumentException("Cluster instances cannot be less than 1 but was " + clusterInstances);
        this.clusterInstances = clusterInstances;
        return this;
    }

    public boolean isUndeployOnComplete() {
        return undeployOnComplete;
    }

    public AgentOptions setUndeployOnComplete(boolean undeployOnComplete) {
        this.undeployOnComplete = undeployOnComplete;
        return this;
    }

    public int getMaxInstancesOnNode() {
        return maxInstancesOnNode;
    }

    public AgentOptions setMaxInstancesOnNode(int maxInstancesOnNode) {
        this.maxInstancesOnNode = maxInstancesOnNode;
        return this;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    public interface AgentRestartStrategy {
        void restart(Runnable agentRunner);
    }
    public static final class AlwaysRestart implements AgentRestartStrategy {
        public static final AlwaysRestart INSTANCE = new AlwaysRestart();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof AlwaysRestart;
        }

        @Override
        public void restart(Runnable agentRunner) {
            agentRunner.run();
        }
    }
    public static final class NeverRestart implements AgentRestartStrategy {
        public static final AgentRestartStrategy INSTANCE = new RestartTimes(0);

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof NeverRestart;
        }

        @Override
        public void restart(Runnable agentRunner) {
            //no op
        }

    }
    public static final class RestartTimes implements AgentRestartStrategy {
        public final int times;
        private int timesRestarted = 0;

        public RestartTimes(int times) {
            this.times = times;
        }

        public RestartTimes of(int noOfTimes) {
            return new RestartTimes(noOfTimes);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RestartTimes)) return false;
            final RestartTimes that = (RestartTimes) o;
            return times == that.times;
        }

        @Override
        public int hashCode() {
            return Objects.hash(times);
        }

        @Override
        public void restart(Runnable agentRunner) {
            if (timesRestarted++ < times) {
                agentRunner.run();
            }
        }
    }

    public AgentRestartStrategy getAgentRestartStrategy() {
        return agentRestartStrategy;
    }

    public AgentOptions setAgentRestartStrategy(AgentRestartStrategy agentRestartStrategy) {
        this.agentRestartStrategy = agentRestartStrategy;
        return this;
    }

}
