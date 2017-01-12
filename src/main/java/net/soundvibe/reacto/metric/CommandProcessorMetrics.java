package net.soundvibe.reacto.metric;

import java.lang.management.*;
import java.util.*;

/**
 * @author OZY on 2017.01.12.
 */
public final class CommandProcessorMetrics {

    private final Collection<CommandProcessorMetric> elements;

    public CommandProcessorMetrics(Collection<CommandProcessorMetric> elements) {
        this.elements = elements;
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

    public MemoryUsage memoryUsage() {
        return ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
    }

    @Override
    public String toString() {
        return "CommandProcessorMetrics{" +
                "elements=" + elements +
                ",memoryUsage=" + memoryUsage() +
                '}';
    }
}
