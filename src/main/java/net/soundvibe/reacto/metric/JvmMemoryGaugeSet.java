package net.soundvibe.reacto.metric;

import com.codahale.metrics.*;

import java.lang.management.*;
import java.util.*;

/**
 * @author linas on 17.2.10.
 */
public class JvmMemoryGaugeSet implements MetricSet {

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();
        final MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();

        gauges.put("committed", (Gauge<Long>) heapMemoryUsage::getCommitted);
        gauges.put("init", (Gauge<Long>) heapMemoryUsage::getInit);
        gauges.put("max", (Gauge<Long>) heapMemoryUsage::getMax);
        gauges.put("used", (Gauge<Long>) heapMemoryUsage::getUsed);

        return Collections.unmodifiableMap(gauges);
    }
}
