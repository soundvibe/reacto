package net.soundvibe.reacto.mappers.jackson;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import net.soundvibe.reacto.discovery.types.*;
import net.soundvibe.reacto.mappers.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.*;

import java.io.*;
import java.util.*;

/**
 * @author Linas on 2017.01.10.
 */
public final class JacksonMapper implements ServiceRegistryMapper, CommandRegistryMapper {

    private final ObjectMapper objectMapper;

    public final static ObjectMapper JSON = new ObjectMapper();

    static {
        JSON.registerModule(jsonTypesModule());
    }

    public static String toJson(Object object) {
        try {
            return JSON.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T fromJson(String json, Class<T> aClass) {
        try {
            return JSON.readValue(json, aClass);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public JacksonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static SimpleModule jsonTypesModule() {
        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(JsonObject.class, new JsonObject.JsonObjectSerializer());
        simpleModule.addSerializer(JsonArray.class, new JsonArray.JsonArraySerializer());
        simpleModule.addDeserializer(JsonObject.class, new JsonObject.JsonObjectDeserializer());
        simpleModule.addDeserializer(JsonArray.class, new JsonArray.JsonArrayDeserializer());
        simpleModule.addDeserializer(ServiceRecord.class, new ServiceRecordDeserializer());
        return simpleModule;
    }

    @Override
    public <C, E> TypedCommand toCommand(C genericCommand, Class<? extends E> eventClass) {
        try {
            return TypedCommand.create(
                        genericCommand.getClass(),
                        eventClass,
                        objectMapper.writeValueAsBytes(genericCommand)
            );
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <E> E toGenericEvent(Event event, Class<? extends E> eventClass) {
        try {
            return objectMapper.readValue(
                    event.payload.orElseThrow(() -> new IllegalStateException("Payload is missing for " + event)),
                    eventClass);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <C> C toGenericCommand(Command command, Class<? extends C> commandClass) {
        try {
            return objectMapper.readValue(
                    command.payload.orElseThrow(() -> new IllegalStateException("Payload is missing for " + command)),
                    commandClass);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public <E> TypedEvent toEvent(E genericEvent) {
        try {
            return TypedEvent.create(
                    genericEvent.getClass(),
                    objectMapper.writeValueAsBytes(genericEvent)
            );
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }


    public static class ServiceRecordDeserializer extends JsonDeserializer<ServiceRecord> {

        private static final JavaType mapType = TypeFactory.defaultInstance().constructMapType(
                HashMap.class, String.class, Object.class);

        private String getString(Map<String, Object> map, String key) {
            final Object value = map.get(key);
            if (value instanceof String) {
                return (String) value;
            }
            return null;
        }

        private Status getStatus(Map<String, Object> map) {
            try {
                return Optional.ofNullable(map.get("status"))
                        .filter(o -> o instanceof String)
                        .map(o -> Status.valueOf((String) o))
                        .orElse(Status.UNKNOWN);
            } catch (Throwable e) {
                return Status.UNKNOWN;
            }
        }

        private ServiceType getServiceType(Map<String, Object> map) {
            try {
                return Optional.ofNullable(map.get("type"))
                        .filter(o -> o instanceof String)
                        .map(o -> ServiceType.valueOf((String) o))
                        .orElse(ServiceType.LOCAL);
            } catch (Throwable e) {
                return ServiceType.LOCAL;
            }
        }

        @SuppressWarnings("unchecked")
        private JsonObject getObject(Map<String, Object> map, String key) {
            final Object value = map.get(key);
            if (value instanceof Map) {
                return new JsonObject((Map<String, Object>) value);
            } else if (value instanceof JsonObject) {
                return (JsonObject) value;
            }
            return JsonObject.empty();
        }

        @Override
        public ServiceRecord deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            final Map<String, Object> map = ctxt.readValue(p, mapType);
            return ServiceRecord.create(
                    getString(map, "name"),
                    getStatus(map),
                    getServiceType(map),
                    getString(map, "registrationId"),
                    getObject(map, "location"),
                    getObject(map, "metadata")
                    );
        }
    }

}
