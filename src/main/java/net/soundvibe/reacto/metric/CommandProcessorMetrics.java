package net.soundvibe.reacto.metric;

import java.lang.management.*;
import java.util.*;

/**
 * @author OZY on 2017.01.12.
 */
public final class CommandProcessorMetrics {

    private final Collection<CommandProcessorMetric> elements;
    public final long delayInMs;

    public CommandProcessorMetrics(Collection<CommandProcessorMetric> elements, long delayInMs) {
        this.elements = elements;
        this.delayInMs = delayInMs;
    }

    public Collection<CommandProcessorMetric> commands() {
        return elements;
    }

    public CommandProcessorMetric getCommand(int index) {
        int i = 0;
        for (CommandProcessorMetric element : elements) {
            if (i == index) return element;
            i++;
        }
        throw new NoSuchElementException("No such element at " + index);
    }

    static public MemoryUsage memoryUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }

    static public ThreadData threadUsage() {
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        return new ThreadData(
                threadMXBean.getThreadCount(),
                threadMXBean.getDaemonThreadCount(),
                threadMXBean.getPeakThreadCount());
    }

    @Override
    public String toString() {
        return "CommandProcessorMetrics{" +
                "elements=" + elements +
                ",delayInMs=" + delayInMs +
                ",memoryUsage=" + memoryUsage() +
                ",threadUsage=" + threadUsage() +
                '}';
    }
}
