package reactive.fp.mappers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import reactive.fp.client.commands.DistributedCommandDef;
import reactive.fp.client.events.EventHandler;
import reactive.fp.client.events.EventHandlers;
import reactive.fp.types.*;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
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
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);
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
            return Mappers.jsonMapper.readValue(bytes, Event.class);
        } catch (IOException e) {
            throw new RuntimeJsonMappingException("Cannot deserialize event from json: " + Arrays.toString(bytes));
        }
    }

    static Command<?> fromJsonToCommand(byte[] bytes) {
        try {
            return Mappers.jsonMapper.readValue(bytes, Command.class);
        } catch (IOException e) {
            throw new RuntimeJsonMappingException("Cannot deserialize command from json: " + Arrays.toString(bytes));
        }
    }

    static <T> Optional<EventHandlers<T>> mapToEventHandlers(DistributedCommandDef distributedCommandDef,
                                                             Function<URI, EventHandler<T>> eventHandlerFactory) {
        return Optional.ofNullable(distributedCommandDef.mainURI())
                .map(eventHandlerFactory::apply)
                .map(mainEventHandler -> new EventHandlers<>(mainEventHandler, Optional.empty()))
                .map(eventHandlers -> distributedCommandDef.fallbackURI()
                        .map(eventHandlerFactory::apply)
                        .map(eventHandlers::copy)
                        .orElse(eventHandlers));
    }

}
