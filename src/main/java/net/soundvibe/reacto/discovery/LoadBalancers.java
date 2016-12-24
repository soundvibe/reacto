package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.client.events.EventHandler;

/**
 * @author OZY on 2016.08.26.
 */
public interface LoadBalancers {

    LoadBalancer<EventHandler> RANDOM = new RandomLoadBalancer<>();

    LoadBalancer<EventHandler> ROUND_ROBIN = new RoundRobinLoadBalancer<>();

}
