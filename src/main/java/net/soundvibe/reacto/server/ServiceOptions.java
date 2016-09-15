package net.soundvibe.reacto.server;

import net.soundvibe.reacto.discovery.DiscoverableService;

import java.util.Optional;

/**
 * @author OZY on 2016.08.29.
 */
public final class ServiceOptions {

    public final String serviceName;
    public final String root;
    public final String version;
    public final Optional<DiscoverableService> serviceDiscovery;

    public ServiceOptions(String serviceName, String root) {
        this.serviceName = serviceName;
        this.root = root;
        this.version = "UNKNOWN";
        this.serviceDiscovery = Optional.empty();
    }

    public ServiceOptions(String serviceName, String root, DiscoverableService discoverableService) {
        this.serviceName = serviceName;
        this.root = root;
        this.version = "UNKNOWN";
        this.serviceDiscovery = Optional.of(discoverableService);
    }

    public ServiceOptions(String serviceName, String root, String version) {
        this.serviceName = serviceName;
        this.root = root;
        this.version = version;
        this.serviceDiscovery = Optional.empty();
    }

    public ServiceOptions(String serviceName, String root, String version, DiscoverableService discoverableService) {
        this.serviceName = serviceName;
        this.root = root;
        this.version = version;
        this.serviceDiscovery = Optional.of(discoverableService);
    }

    @Override
    public String toString() {
        return "ServiceOptions{" +
                "serviceName='" + serviceName + '\'' +
                ", root='" + root + '\'' +
                ", version='" + version + '\'' +
                ", serviceDiscovery=" + serviceDiscovery +
                '}';
    }
}
