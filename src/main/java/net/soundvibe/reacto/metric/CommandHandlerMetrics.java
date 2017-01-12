package net.soundvibe.reacto.metric;

import java.lang.management.*;
import java.util.List;

/**
 * @author OZY on 2017.01.12.
 */
public final class CommandHandlerMetrics {

    private final List<CommandHandlerMetric> elements;

    public CommandHandlerMetrics(List<CommandHandlerMetric> elements) {
        this.elements = elements;
    }

    public List<CommandHandlerMetric> commands() {
        return elements;
    }

    public MemoryUsage memoryUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }

    @Override
    public String toString() {
        return "CommandHandlerMetrics{" +
                "elements=" + elements +
                ",memoryUsage=" + memoryUsage() +
                '}';
    }
}
