package net.soundvibe.reacto.types;

import net.soundvibe.reacto.internal.ObjectId;

import java.util.*;

/**
 * @author OZY on 2017.01.10.
 */
public final class TypedCommand extends Command {

    private TypedCommand(ObjectId id, String name, Optional<MetaData> metaData, Optional<byte[]> payload) {
        super(id, name, metaData, payload);
    }

    public static TypedCommand create(Class<?> commandType, Class<?> eventType, byte[] serializedCommand) {
        Objects.requireNonNull(commandType, "commandType cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(serializedCommand, "serializedCommand cannot be null");
        return new TypedCommand(ObjectId.get(), commandType.getName(),
                Optional.of(MetaData.of(CommandDescriptor.EVENT, eventType.getName())),
                Optional.of(serializedCommand));
    }

    public static TypedCommand create(Class<?> commandType, Class<?> eventType, MetaData metaData) {
        Objects.requireNonNull(commandType, "commandType cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(metaData, "metaData cannot be null");
        return new TypedCommand(ObjectId.get(), commandType.getName(),
                Optional.of(MetaData.of(CommandDescriptor.EVENT, eventType.getName()).concat(metaData)),
                Optional.empty());
    }

    public static TypedCommand create(Class<?> commandType, Class<?> eventType, MetaData metaData, byte[] serializedCommand) {
        Objects.requireNonNull(commandType, "commandType cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        Objects.requireNonNull(metaData, "metaData cannot be null");
        Objects.requireNonNull(serializedCommand, "serializedCommand cannot be null");
        return new TypedCommand(ObjectId.get(), commandType.getName(),
                Optional.of(MetaData.of(CommandDescriptor.EVENT, eventType.getName()).concat(metaData)),
                Optional.of(serializedCommand));
    }

    public static TypedCommand create(Class<?> commandType, Class<?> eventType, Command command) {
        Objects.requireNonNull(commandType, "commandType cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        return new TypedCommand(
                ObjectId.get(),
                commandType.getName(),
                Optional.of(MetaData.of(CommandDescriptor.EVENT, eventType.getName()).concat(command.metaData.orElse(MetaData.empty()))),
                command.payload);
    }

    public String commandType() {
        return name;
    }

    public String eventType() {
        return get(CommandDescriptor.EVENT);
    }
}
