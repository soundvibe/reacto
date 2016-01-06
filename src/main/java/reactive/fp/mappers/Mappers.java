package reactive.fp.mappers;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import reactive.fp.client.commands.CommandDef;
import reactive.fp.client.events.EventHandler;
import reactive.fp.client.events.EventHandlers;
import reactive.fp.types.Command;
import reactive.fp.client.errors.CommandError;
import reactive.fp.types.Event;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Linas on 2015.10.25.
 */
public interface Mappers {

    static ObjectMapper createJsonMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModules(
                new Jdk8Module(),
                new JavaTimeModule()
        );
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.configure(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS, true);
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        return objectMapper;
    }

    ObjectMapper jsonMapper = createJsonMapper();

    static <T> byte[] messageToJsonBytes(T message) {
        try {
            return jsonMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeJsonMappingException("Cannot serialize message to json: " + message);
        }
    }

    static <T> String messageToJsonString(T message) {
        try {
            return jsonMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new RuntimeJsonMappingException("Cannot serialize message to json: " + message);
        }
    }

    static Event<?> fromJsonToEvent(byte[] bytes) {
        try {
            return jsonMapper.readValue(bytes, Event.class);
        } catch (IOException e) {
            throw new RuntimeJsonMappingException("Cannot deserialize event from json: " + new String(bytes));
        }
    }

    static Command<?> fromJsonToCommand(byte[] bytes) {
        try {
            return jsonMapper.readValue(bytes, Command.class);
        } catch (IOException e) {
            throw new RuntimeJsonMappingException("Cannot deserialize command from json: " + new String(bytes));
        }
    }

    static <T> Optional<EventHandlers<T>> mapToEventHandlers(CommandDef commandDef,
                                                             Function<URI, EventHandler<T>> eventHandlerFactory) {
        return Optional.ofNullable(commandDef.mainURI())
                .map(eventHandlerFactory::apply)
                .map(mainEventHandler -> new EventHandlers<>(mainEventHandler, Optional.empty()))
                .map(eventHandlers -> commandDef.fallbackURI()
                        .map(eventHandlerFactory::apply)
                        .map(eventHandlers::copy)
                        .orElse(eventHandlers));
    }

    @SuppressWarnings("unchecked")
    static Throwable mapToThrowable(Object error) {
        if (error instanceof Throwable) {
            return (Throwable) error;
        } else if (error instanceof Map) {
            return new CommandError(((Map<String, String>) error).get("message"));
        } else if (error instanceof String) {
            return new CommandError((String) error);
        }
        return new CommandError("Unknown Error");
    }

}
