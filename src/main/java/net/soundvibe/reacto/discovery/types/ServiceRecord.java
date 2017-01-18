package net.soundvibe.reacto.discovery.types;

import net.soundvibe.reacto.internal.ObjectId;
import net.soundvibe.reacto.types.json.*;
import net.soundvibe.reacto.utils.WebUtils;

import java.util.Objects;

/**
 * @author Linas on 2017.01.17.
 */
public final class ServiceRecord {

    public static final String LOCATION_HOST = "host";
    public static final String LOCATION_PORT = "port";
    public static final String LOCATION_ROOT = "root";
    public static final String METADATA_VERSION = "version";
    public static final String METADATA_COMMANDS = "commands";
    public final String name;
    public final Status status;
    public final ServiceType type;
    public final String registrationId;
    public final JsonObject location;
    public final JsonObject metaData;

    private ServiceRecord(String name, Status status, ServiceType type, String registrationId,
                          JsonObject location,
                          JsonObject metaData) {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(type, "type cannot be null");
        Objects.requireNonNull(registrationId, "registrationId cannot be null");
        Objects.requireNonNull(location, "location cannot be null");
        Objects.requireNonNull(metaData, "metaData cannot be null");
        this.name = name;
        this.status = status;
        this.type = type;
        this.registrationId = registrationId;
        this.location = location;
        this.metaData = metaData;
    }

    public static JsonObject httpEndpointLocation(String host, int port, String root) {
        return JsonObjectBuilder.create()
                .put(LOCATION_HOST, host)
                .put(LOCATION_PORT, port)
                .put(LOCATION_ROOT, root)
                .build();
    }

    public static ServiceRecord createHttpEndpoint(String name, int port, String root, String version) {
        return ServiceRecord.create(
                name,
                net.soundvibe.reacto.discovery.types.Status.UP,
                ServiceType.HTTP_ENDPOINT,
                ObjectId.get().toString(),
                ServiceRecord.httpEndpointLocation(WebUtils.getLocalAddress(), port, root),
                JsonObjectBuilder.create()
                        .put(ServiceRecord.METADATA_VERSION, version)
                        .build()
        );
    }

    public static ServiceRecord create(String name,
                                       Status status,
                                       ServiceType type,
                                       JsonObject location,
                                       JsonObject metaData) {
        return new ServiceRecord(name, status, type, ObjectId.get().toString(), location, metaData);
    }

    public static ServiceRecord create(String name,
                                       Status status,
                                       ServiceType type,
                                       String registrationId,
                                       JsonObject location,
                                       JsonObject metaData) {
        return new ServiceRecord(name, status, type, registrationId, location, metaData);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServiceRecord that = (ServiceRecord) o;
        return Objects.equals(name, that.name) &&
                status == that.status &&
                type == that.type &&
                Objects.equals(registrationId, that.registrationId) &&
                Objects.equals(location, that.location) &&
                Objects.equals(metaData, that.metaData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, status, type, registrationId, location, metaData);
    }

    @Override
    public String toString() {
        return "ServiceRecord{" +
                "name='" + name + '\'' +
                ", status=" + status +
                ", type=" + type +
                ", registrationId='" + registrationId + '\'' +
                ", location=" + location +
                ", metaData=" + metaData +
                '}';
    }
}
