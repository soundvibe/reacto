package net.soundvibe.reacto.client.commands;

import io.vertx.servicediscovery.ServiceDiscovery;

import java.util.*;

/**
 * @author OZY on 2016.07.15.
 */
public final class Services {

    public final String serviceName;
    public final ServiceDiscovery serviceDiscovery;

    private Services(String serviceName, ServiceDiscovery serviceDiscovery) {
        this.serviceName = serviceName;
        this.serviceDiscovery = serviceDiscovery;
    }

    public static Services of(String serviceName, ServiceDiscovery serviceDiscovery) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        Objects.requireNonNull(serviceDiscovery, "serviceDiscovery cannot be null");
        return new Services(serviceName, serviceDiscovery);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Services services = (Services) o;
        return Objects.equals(serviceName, services.serviceName) &&
                Objects.equals(serviceDiscovery, services.serviceDiscovery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, serviceDiscovery);
    }

    @Override
    public String toString() {
        return "Services{" +
                "serviceName='" + serviceName + '\'' +
                ", serviceDiscovery=" + serviceDiscovery +
                '}';
    }
}
