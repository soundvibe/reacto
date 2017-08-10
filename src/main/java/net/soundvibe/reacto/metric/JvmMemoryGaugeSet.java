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

        gauges.put("heap.committed", (Gauge<Long>) heapMemoryUsage::getCommitted);
        gauges.put("heap.init", (Gauge<Long>) heapMemoryUsage::getInit);
        gauges.put("heap.max", (Gauge<Long>) heapMemoryUsage::getMax);
        gauges.put("heap.used", (Gauge<Long>) heapMemoryUsage::getUsed);

        final MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();
        gauges.put("non-heap.committed", (Gauge<Long>) nonHeapMemoryUsage::getCommitted);
        gauges.put("non-heap.init", (Gauge<Long>) nonHeapMemoryUsage::getInit);
        gauges.put("non-heap.max", (Gauge<Long>) nonHeapMemoryUsage::getMax);
        gauges.put("non-heap.used", (Gauge<Long>) nonHeapMemoryUsage::getUsed);

        gauges.put("total.committed", (Gauge<Long>) () -> heapMemoryUsage.getCommitted() + nonHeapMemoryUsage.getCommitted());
        gauges.put("total.init", (Gauge<Long>) () -> heapMemoryUsage.getInit() + nonHeapMemoryUsage.getInit());
        gauges.put("total.max", (Gauge<Long>) () -> heapMemoryUsage.getMax() + nonHeapMemoryUsage.getMax());
        gauges.put("total.used", (Gauge<Long>) () -> heapMemoryUsage.getUsed() + nonHeapMemoryUsage.getUsed());

        return Collections.unmodifiableMap(gauges);
    }
}
