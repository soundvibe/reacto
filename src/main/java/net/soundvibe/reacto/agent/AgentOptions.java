package net.soundvibe.reacto.agent;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.slf4j.*;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;

public abstract class AgentOptions {

    private static final Logger log = LoggerFactory.getLogger(AgentOptions.class);

    private int clusterInstances = 1;
    private final AtomicInteger clusterInstancesCache = new AtomicInteger(clusterInstances);
    private IntSupplier clusterInstancesResolver;
    private int maxInstancesOnNode = 4;
    private boolean isHA = false;
    private OnCompleteAction onCompleteAction = OnCompleteAction.undeploy;
    private AgentRestartStrategy onCompleteRestartStrategy = AlwaysRestart.INSTANCE;
    private AgentRestartStrategy agentRestartStrategy = AlwaysRestart.INSTANCE;

    public boolean isHA() {
        return isHA;
    }

    public AgentOptions setHA(boolean value) {
        this.isHA = value;
        return this;
    }

    public int getClusterInstances() {
        if (clusterInstancesResolver == null) {
            return clusterInstances;
        }

        try {
            final int instances = clusterInstancesResolver.getAsInt();
            if (instances != clusterInstancesCache.get()) {
                clusterInstancesCache.set(instances);
            }
            return instances;
        } catch (Exception e) {
            log.warn("Unable to resolve cluster instances, using default value: {}", clusterInstancesCache.get(), e);
            return clusterInstancesCache.get();
        }
    }

    /**
     * Sets static number of cluster instances.
     * For implementing auto-scalable agents, use {@link AgentOptions}.setClusterInstances({@link IntSupplier} clusterInstancesResolver)
     * @param clusterInstances number of cluster-wide instances to be deployed
     * @return AgentOptions
     */
    public AgentOptions setClusterInstances(int clusterInstances) {
        if (clusterInstances < 1) throw new IllegalArgumentException("Cluster instances cannot be less than 1 but was " + clusterInstances);
        this.clusterInstances = clusterInstances;
        return this;
    }

    /**
     * Sets clusterInstancesResolver, which will be called automatically before deploying new agents.
     * Could be used to implement auto-scalable agents
     * @param clusterInstancesResolver Function to resolve number of cluster instances
     * @return AgentOptions
     */
    public AgentOptions setClusterInstances(IntSupplier clusterInstancesResolver) {
        this.clusterInstancesResolver = clusterInstancesResolver;
        return this;
    }

    public OnCompleteAction getOnCompleteAction() {
        return onCompleteAction;
    }

    public AgentOptions setOnCompleteAction(OnCompleteAction onCompleteAction) {
        this.onCompleteAction = onCompleteAction;
        return this;
    }

    public int getMaxInstancesOnNode() {
        return maxInstancesOnNode;
    }

    public AgentOptions setMaxInstancesOnNode(int maxInstancesOnNode) {
        this.maxInstancesOnNode = maxInstancesOnNode;
        return this;
    }

    public AgentRestartStrategy getOnCompleteRestartStrategy() {
        return onCompleteRestartStrategy;
    }

    public AgentOptions setOnCompleteRestartStrategy(AgentRestartStrategy onCompleteRestartStrategy) {
        this.onCompleteRestartStrategy = onCompleteRestartStrategy;
        return this;
    }

    public enum OnCompleteAction {
        undeploy, restart
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    public interface AgentRestartStrategy {
        boolean restart(Runnable agentRunner);
    }
    public static final class AlwaysRestart implements AgentRestartStrategy {
        public static final AlwaysRestart INSTANCE = new AlwaysRestart();

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            return o instanceof AlwaysRestart;
        }

        @Override
        public boolean restart(Runnable agentRunner) {
            agentRunner.run();
            return true;
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
        public boolean restart(Runnable agentRunner) {
            return false;
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
        public boolean restart(Runnable agentRunner) {
            if (timesRestarted++ < times) {
                agentRunner.run();
                return true;
            } else {
                return false;
            }
        }
    }

    public static final class RestartWithTimeout implements AgentRestartStrategy {

        private final Duration timeout;

        public RestartWithTimeout(Duration timeout) {
            this.timeout = timeout;
        }

        public static RestartWithTimeout of(Duration timeout) {
            return new RestartWithTimeout(timeout);
        }

        @Override
        public boolean restart(Runnable agentRunner) {
            try {
                Thread.sleep(timeout.toMillis());
                agentRunner.run();
                return true;
            } catch (InterruptedException e) {
                return false;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof RestartWithTimeout)) return false;
            final RestartWithTimeout that = (RestartWithTimeout) o;
            return Objects.equals(timeout, that.timeout);
        }

        @Override
        public int hashCode() {
            return Objects.hash(timeout);
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
