package reactive.fp.mappers;

import com.google.protobuf.InvalidProtocolBufferException;
import reactive.fp.client.commands.CommandDef;
import reactive.fp.client.events.EventHandler;
import reactive.fp.client.events.EventHandlers;
import reactive.fp.types.*;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Linas on 2015.10.25.
 */
public interface Mappers {

    static byte[] eventToBytes(Event event) {
        return MessageMappers.toProtoBufEvent(event).toByteArray();
    }

    static byte[] commandToBytes(Command command) {
        return MessageMappers.toProtoBufCommand(command).toByteArray();
    }

    static Event fromBytesToEvent(byte[] bytes) {
        try {
            return MessageMappers.toEvent(Messages.Event.parseFrom(bytes));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeProtocolBufferException("Cannot deserialize event from bytes: " + new String(bytes), e);
        }
    }

    static Command fromBytesToCommand(byte[] bytes) {
        try {
            return MessageMappers.toCommand(Messages.Command.parseFrom(bytes));
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeProtocolBufferException("Cannot deserialize command from bytes: " + new String(bytes), e);
        }
    }

    static <T,U> Optional<EventHandlers> mapToEventHandlers(CommandDef commandDef,
                                                             Function<URI, EventHandler> eventHandlerFactory) {
        return Optional.ofNullable(commandDef.mainURI())
                .map(eventHandlerFactory::apply)
                .map(mainEventHandler -> new EventHandlers(mainEventHandler, Optional.empty()))
                .map(eventHandlers -> commandDef.fallbackURI()
                        .map(eventHandlerFactory::apply)
                        .map(eventHandlers::copy)
                        .orElse(eventHandlers));
    }
}
