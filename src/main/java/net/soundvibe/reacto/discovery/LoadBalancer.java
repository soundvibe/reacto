package net.soundvibe.reacto.discovery;

import java.util.List;

/**
 * @author OZY on 2016.08.26.
 */
public interface LoadBalancer<T> {

    T balance(List<T> records);

}
