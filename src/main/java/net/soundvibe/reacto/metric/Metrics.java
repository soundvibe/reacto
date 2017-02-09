package net.soundvibe.reacto.metric;

import com.codahale.metrics.MetricRegistry;

/**
 * @author linas on 17.2.9.
 */
public interface Metrics {

    MetricRegistry REGISTRY = new MetricRegistry();

}
