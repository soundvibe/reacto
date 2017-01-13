package net.soundvibe.reacto.server;

import java.util.Objects;

/**
 * @author OZY on 2016.08.29.
 */
public final class ServiceOptions {

    public final String serviceName;
    public final String root;
    public final String version;
    public final boolean isSsl;

    public ServiceOptions(String serviceName, String root) {
        this.serviceName = serviceName;
        this.root = root;
        this.version = "UNKNOWN";
        this.isSsl = false;
    }

    public ServiceOptions(String serviceName, String root, String version) {
        this.serviceName = serviceName;
        this.root = root;
        this.version = version;
        this.isSsl = false;
    }

    public ServiceOptions(String serviceName, String root, String version, boolean isSsl) {
        this.serviceName = serviceName;
        this.root = root;
        this.version = version;
        this.isSsl = isSsl;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ServiceOptions that = (ServiceOptions) o;
        return isSsl == that.isSsl &&
                Objects.equals(serviceName, that.serviceName) &&
                Objects.equals(root, that.root) &&
                Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, root, version, isSsl);
    }

    @Override
    public String toString() {
        return "ServiceOptions{" +
                "serviceName='" + serviceName + '\'' +
                ", root='" + root + '\'' +
                ", version='" + version + '\'' +
                ", isSsl=" + isSsl +
                '}';
    }
}
