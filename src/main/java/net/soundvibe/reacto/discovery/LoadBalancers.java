package net.soundvibe.reacto.discovery;

/**
 * @author OZY on 2016.08.26.
 */
public interface LoadBalancers {

    LoadBalancer RANDOM = new RandomLoadBalancer();

    LoadBalancer ROUND_ROBIN = new RoundRobinLoadBalancer();

}
