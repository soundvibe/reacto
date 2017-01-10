package net.soundvibe.reacto.mappers;

import com.google.protobuf.InvalidProtocolBufferException;
import net.soundvibe.reacto.client.events.*;
import net.soundvibe.reacto.internal.*;
import net.soundvibe.reacto.internal.proto.Messages;
import net.soundvibe.reacto.types.*;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Linas on 2015.10.25.
 */
public interface Mappers {

    static byte[] internalEventToBytes(InternalEvent internalEvent) {
        return MessageMappers.toProtoBufEvent(internalEvent).toByteArray();
    }

    static byte[] commandToBytes(Command command) {
        return MessageMappers.toProtoBufCommand(command).toByteArray();
    }

    static InternalEvent fromBytesToInternalEvent(byte[] bytes) {
        try {
            return MessageMappers.toInternalEvent(Messages.Event.parseFrom(bytes));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeProtocolBufferException("Cannot deserialize event from bytes: " + new String(bytes), e);
        }
    }

    static Event fromInternalEvent(InternalEvent internalEvent) {
        return Event.create(internalEvent.name, internalEvent.metaData, internalEvent.payload);
    }

    static Command fromBytesToCommand(byte[] bytes) {
        try {
            return MessageMappers.toCommand(Messages.Command.parseFrom(bytes));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeProtocolBufferException("Cannot deserialize command from bytes: " + new String(bytes), e);
        }
    }

    static Optional<byte[]> exceptionToBytes(Throwable throwable) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(byteArrayOutputStream)) {
            oos.writeObject(throwable);
            return Optional.of(byteArrayOutputStream.toByteArray());
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    static Optional<Throwable> fromBytesToException(byte[] bytes) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            return Optional.ofNullable(objectInputStream.readObject())
                    .map(o -> (Throwable) o);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    static List<EventHandler> mapToEventHandlers(Nodes nodes,
                                                 Function<URI, EventHandler> eventHandlerFactory) {
        return nodes.stream()
                    .map(eventHandlerFactory)
                    .collect(Collectors.toList());
    }

    static ServiceRegistryMapper untypedServiceRegistryMapper() {
        return new ServiceRegistryMapper() {
            @Override
            public <C, E> TypedCommand toCommand(C genericCommand, Class<? extends E> eventClass) {
                if (!genericCommand.getClass().equals(TypedCommand.class)) {
                    throw new IllegalArgumentException("untypedServiceRegistryMapper accepts only basic Command instances. Provided: " + genericCommand.getClass());
                }
                return (TypedCommand) genericCommand;
            }

            @Override
            public <E> E toGenericEvent(Event event, Class<? extends E> eventClass) {
                if (!eventClass.equals(Event.class)) {
                    throw new IllegalArgumentException("untypedServiceRegistryMapper emits only basic Events. Provided: " + eventClass);
                }
                return eventClass.cast(event);
            }
        };
    }

    static CommandRegistryMapper untypedCommandRegistryMapper() {
        return new CommandRegistryMapper() {
            @Override
            public <C> C toGenericCommand(Command command, Class<? extends C> commandClass) {
                if (!commandClass.isInstance(command)) {
                    throw new IllegalArgumentException("untypedCommandRegistryMapper accepts only basic Command instances. Provided: " + commandClass);
                }
                return commandClass.cast(command);
            }

            @Override
            public <E> TypedEvent toEvent(E genericEvent) {
                if (!genericEvent.getClass().equals(TypedEvent.class)) {
                    throw new IllegalArgumentException("untypedCommandRegistryMapper emits only basic Events. Provided: " + genericEvent.getClass());
                }
                return (TypedEvent) genericEvent;
            }
        };
    }
}
