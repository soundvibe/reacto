package net.soundvibe.reacto.server.vertx;

import io.vertx.servicediscovery.ServiceDiscovery;

import java.util.*;

/**
 * @author OZY on 2016.07.15.
 */
public final class Service {

    public final String name;
    public final ServiceDiscovery serviceDiscovery;

    private Service(String name, ServiceDiscovery serviceDiscovery) {
        this.name = name;
        this.serviceDiscovery = serviceDiscovery;
    }

    public static Service of(String serviceName, ServiceDiscovery serviceDiscovery) {
        Objects.requireNonNull(serviceName, "name cannot be null");
        Objects.requireNonNull(serviceDiscovery, "serviceDiscovery cannot be null");
        return new Service(serviceName, serviceDiscovery);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Service service = (Service) o;
        return Objects.equals(name, service.name) &&
                Objects.equals(serviceDiscovery, service.serviceDiscovery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, serviceDiscovery);
    }

    @Override
    public String toString() {
        return "Service{" +
                "name='" + name + '\'' +
                ", serviceDiscovery=" + serviceDiscovery +
                '}';
    }
}
