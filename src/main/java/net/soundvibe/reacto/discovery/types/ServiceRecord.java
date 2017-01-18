package net.soundvibe.reacto.discovery.types;

import net.soundvibe.reacto.internal.ObjectId;
import net.soundvibe.reacto.types.json.JsonObject;

import java.util.Objects;

/**
 * @author Linas on 2017.01.17.
 */
public final class ServiceRecord {

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

/*    public static ServiceRecord httpEndpoint(String name,
                                       Status status,
                                       URI uri, JsonObject metaData) {
        return new ServiceRecord(name, status, ServiceType.HTTP_ENDPOINT, ObjectId.get().toString(),
                uriToLocation(uri), metaData);
    }

    private static JsonObject uriToLocation(URI uri) {
        final Map<String, Object> decode = new LinkedHashMap<>(10);
        decode.put("host", uri.getHost());
        decode.put("port", uri.getPort());
        decode.put("endpoint", uri.toString());
        decode.put("path", uri.getPath());
        return new JsonObject(decode);
    }*/

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
