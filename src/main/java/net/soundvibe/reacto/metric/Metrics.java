package net.soundvibe.reacto.metric;

import com.codahale.metrics.*;

/**
 * @author linas on 17.2.9.
 */
public interface Metrics {

    MetricRegistry REGISTRY = SharedMetricRegistries.getOrCreate("default");;

    Initializer INITIALIZER = new Initializer();

    class Initializer {

        static {
            REGISTRY.register("jvmAttributes", new JvmAttributeGaugeSet());
            REGISTRY.register("jvmThreads", new JvmThreadGaugeSet());
            REGISTRY.register("jvmMemory", new JvmMemoryGaugeSet());
        }

    }

}
