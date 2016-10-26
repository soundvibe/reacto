package net.soundvibe.reacto.client.commands;

import io.vertx.servicediscovery.ServiceDiscovery;

import java.util.*;

/**
 * @author OZY on 2016.07.15.
 */
public final class Services {

    public final String mainServiceName;
    public final Optional<String> fallbackServiceName;
    public final ServiceDiscovery serviceDiscovery;

    private Services(String mainServiceName, Optional<String> fallbackServiceName, ServiceDiscovery serviceDiscovery) {
        this.mainServiceName = mainServiceName;
        this.fallbackServiceName = fallbackServiceName;
        this.serviceDiscovery = serviceDiscovery;
    }

    public static Services ofMain(String mainServiceName, ServiceDiscovery serviceDiscovery) {
        Objects.requireNonNull(mainServiceName, "mainServiceName cannot be null");
        Objects.requireNonNull(serviceDiscovery, "serviceDiscovery cannot be null");
        return new Services(mainServiceName, Optional.empty(), serviceDiscovery);
    }

    public static Services ofMainAndFallback(String mainServiceName, String fallbackServiceName, ServiceDiscovery serviceDiscovery) {
        Objects.requireNonNull(mainServiceName, "mainServiceName cannot be null");
        Objects.requireNonNull(fallbackServiceName, "fallbackServiceName cannot be null");
        Objects.requireNonNull(serviceDiscovery, "serviceDiscovery cannot be null");
        return new Services(mainServiceName, Optional.of(fallbackServiceName), serviceDiscovery);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Services services = (Services) o;
        return Objects.equals(mainServiceName, services.mainServiceName) &&
                Objects.equals(fallbackServiceName, services.fallbackServiceName) &&
                Objects.equals(serviceDiscovery, services.serviceDiscovery);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainServiceName, fallbackServiceName, serviceDiscovery);
    }

    @Override
    public String toString() {
        return "Services{" +
                "mainServiceName='" + mainServiceName + '\'' +
                ", fallbackServiceName=" + fallbackServiceName +
                ", serviceDiscovery=" + serviceDiscovery +
                '}';
    }
}
