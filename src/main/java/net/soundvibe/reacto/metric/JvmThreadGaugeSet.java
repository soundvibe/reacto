package net.soundvibe.reacto.metric;

import com.codahale.metrics.*;

import java.lang.management.*;
import java.util.*;

/**
 * @author linas on 17.2.10.
 */
public class JvmThreadGaugeSet implements MetricSet {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Override
    public Map<String, Metric> getMetrics() {
        final Map<String, Metric> gauges = new HashMap<>();

        gauges.put("threadCount", (Gauge<Integer>) threadMXBean::getThreadCount);
        gauges.put("daemonThreadCount", (Gauge<Integer>) threadMXBean::getDaemonThreadCount);
        gauges.put("peakThreadCount", (Gauge<Integer>) threadMXBean::getPeakThreadCount);

        return Collections.unmodifiableMap(gauges);
    }
}
