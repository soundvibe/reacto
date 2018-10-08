package net.soundvibe.reacto.metric;

import com.codahale.metrics.*;
import com.codahale.metrics.jvm.*;

/**
 * @author linas on 17.2.9.
 */
public interface Metrics {

    MetricRegistry REGISTRY = SharedMetricRegistries.getOrCreate("default");;

    Initializer INITIALIZER = new Initializer();

    class Initializer {

        static {
            REGISTRY.register("jvm.Attributes", new JvmAttributeGaugeSet());
            REGISTRY.register("jvm.Threads", new ThreadStatesGaugeSet());
            REGISTRY.register("jvm.Memory", new MemoryUsageGaugeSet());
            REGISTRY.register("jvm.Classes", new ClassLoadingGaugeSet());
        }

    }

}
