package net.soundvibe.reacto.discovery;

import net.soundvibe.reacto.client.events.CommandHandler;

/**
 * @author OZY on 2016.08.26.
 */
public interface LoadBalancers {

    LoadBalancer<CommandHandler> RANDOM = new RandomLoadBalancer<>();

    LoadBalancer<CommandHandler> ROUND_ROBIN = new RoundRobinLoadBalancer<>();

}
