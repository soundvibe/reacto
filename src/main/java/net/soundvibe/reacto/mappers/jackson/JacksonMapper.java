package net.soundvibe.reacto.mappers.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.soundvibe.reacto.mappers.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.*;

import java.io.*;

/**
 * @author Linas on 2017.01.10.
 */
public final class JacksonMapper implements ServiceRegistryMapper, CommandRegistryMapper {

    private final ObjectMapper objectMapper;

    public JacksonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public static SimpleModule jsonTypesModule() {
        final SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(JsonObject.class, new JsonObject.JsonObjectSerializer());
        simpleModule.addSerializer(JsonArray.class, new JsonArray.JsonArraySerializer());
        simpleModule.addDeserializer(JsonObject.class, new JsonObject.JsonObjectDeserializer());
        simpleModule.addDeserializer(JsonArray.class, new JsonArray.JsonArrayDeserializer());
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

}
