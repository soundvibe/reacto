package net.soundvibe.reacto.server;

/**
 * @author OZY on 2016.08.29.
 */
public final class ServiceOptions {

    public final String serviceName;
    public final String root;
    public final String version;

    public ServiceOptions(String serviceName, String root) {
        this.serviceName = serviceName;
        this.root = root;
        this.version = "UNKNOWN";
    }

    public ServiceOptions(String serviceName, String root, String version) {
        this.serviceName = serviceName;
        this.root = root;
        this.version = version;
    }

    @Override
    public String toString() {
        return "ServiceOptions{" +
                "serviceName='" + serviceName + '\'' +
                ", root='" + root + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
