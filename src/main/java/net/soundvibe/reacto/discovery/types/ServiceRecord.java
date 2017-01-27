package net.soundvibe.reacto.discovery.types;

import net.soundvibe.reacto.internal.ObjectId;
import net.soundvibe.reacto.server.*;
import net.soundvibe.reacto.types.CommandDescriptor;
import net.soundvibe.reacto.types.json.*;
import net.soundvibe.reacto.utils.WebUtils;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static net.soundvibe.reacto.types.CommandDescriptor.*;
import static net.soundvibe.reacto.utils.WebUtils.*;

/**
 * @author Linas on 2017.01.17.
 */
public final class ServiceRecord {

    public static final String LOCATION_HOST = "host";
    public static final String LOCATION_PORT = "port";
    public static final String LOCATION_ROOT = "root";
    public static final String LOCATION_SSL = "ssl";
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

    private static JsonObject httpEndpointLocation(String host, int port, String root, boolean isSsl) {
        return JsonObjectBuilder.create()
                .put(LOCATION_HOST, host)
                .put(LOCATION_PORT, port)
                .put(LOCATION_ROOT, root)
                .put(LOCATION_SSL, isSsl)
                .build();
    }

    public static ServiceRecord createWebSocketEndpoint(
            ServiceOptions serviceOptions,
            CommandRegistry commandsToRegister) {
        return createWebSocketEndpoint(
                serviceOptions,
                commandsToRegister.streamOfKeys().collect(toList()));
    }

    public static ServiceRecord createWebSocketEndpoint(
            ServiceOptions serviceOptions,
            Collection<CommandDescriptor> commandsToRegister) {
        return ServiceRecord.create(
                excludeEndDelimiter(excludeStartDelimiter(serviceOptions.serviceName)),
                Status.UP,
                ServiceType.WEBSOCKET,
                ObjectId.get().toString(),
                ServiceRecord.httpEndpointLocation(
                        WebUtils.getLocalAddress(),
                        serviceOptions.port,
                        includeEndDelimiter(includeStartDelimiter(serviceOptions.root)),
                        serviceOptions.isSsl),
                JsonObjectBuilder.create()
                        .put(ServiceRecord.METADATA_VERSION, serviceOptions.version)
                        .putArray(ServiceRecord.METADATA_COMMANDS,
                                arrayBuilder -> {
                                    commandsToRegister.stream()
                                            .map(commandDescriptor -> JsonObjectBuilder.create()
                                                    .put(COMMAND, commandDescriptor.commandType)
                                                    .put(EVENT, commandDescriptor.eventType)
                                                    .build())
                                            .forEach(arrayBuilder::add);
                                    return arrayBuilder;
                                })
                        .build()
        );
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

    private int hash = 0;

    @Override
    public int hashCode() {
        if (hash == 0) {
            hash = Objects.hash(name, status, type, registrationId, location, metaData);
        }
        return hash;
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
