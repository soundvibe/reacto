package reactive.fp.types;

import com.google.protobuf.ByteString;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * @author OZY on 2016.02.05.
 */
public final class MessageMappers {

    public static Command toCommand(Messages.Command protoBufCommand) {
        final Stream<Pair> pairStream = protoBufCommand.getMetadataList().stream()
                .map(o -> Pair.of(o.getKey(), o.getValue()));

        return new Command(
                new ObjectId(protoBufCommand.getId()),
                protoBufCommand.getName(),
                protoBufCommand.getMetadataCount() == 0 ? Optional.empty() : Optional.of(MetaData.fromStream(pairStream)),
                protoBufCommand.getPayload().isEmpty() ? Optional.empty() : Optional.ofNullable(protoBufCommand.getPayload().toByteArray()));
    }

    public static Event toEvent(Messages.Event protoBufEvent) {
        final Stream<Pair> pairStream = protoBufEvent.getMetadataList().stream()
                .map(o -> Pair.of(o.getKey(), o.getValue()));

        return new Event(protoBufEvent.getName(),
                protoBufEvent.getMetadataCount() == 0 ? Optional.empty() : Optional.of(MetaData.fromStream(pairStream)),
                protoBufEvent.getPayload().isEmpty() ? Optional.empty() : Optional.ofNullable(protoBufEvent.getPayload().toByteArray()),
                protoBufEvent.hasError() ?
                        ofNullable(protoBufEvent.getError())
                            .map(error -> new ReactiveException(error.getClassName(), error.getErrorMessage(), error.getStackTrace())):
                        Optional.empty(),
                ofNullable(protoBufEvent.getEventType())
                        .map(eventType -> EventType.valueOf(eventType.name())).orElse(EventType.ERROR));
    }

    public static Messages.Command toProtoBufCommand(Command command) {
        final Messages.Command.Builder commandBuilder = Messages.Command.newBuilder();

        command.metaData.ifPresent(metaData -> {
            final Messages.Metadata.Builder metaDataBuilder = Messages.Metadata.newBuilder();
            commandBuilder.addAllMetadata(metaData.stream()
                    .map(pair -> metaDataBuilder.setKey(pair.key).setValue(pair.value).build())
                    .collect(Collectors.toList()));
        });

        command.payload.ifPresent(bytes -> commandBuilder.setPayload(ByteString.copyFrom(bytes)));
        return commandBuilder.setId(command.id.toString())
                .setName(command.name)
                .build();
    }

    public static Messages.Event toProtoBufEvent(Event event) {
        final Messages.Event.Builder eventBuilder = Messages.Event.newBuilder();
        eventBuilder.setName(event.name);
        eventBuilder.setEventType(Messages.EventType.valueOf(event.eventType.name()));
        event.error.ifPresent(e -> eventBuilder.setError(Messages.Error.newBuilder()
                .setClassName(e.className)
                .setErrorMessage(e.message)
                .setStackTrace(e.stackTrace)
        ));
        event.metaData.ifPresent(metadata -> {
            final Messages.Metadata.Builder metaDataBuilder = Messages.Metadata.newBuilder();
            eventBuilder.addAllMetadata(metadata.stream().map(pair -> metaDataBuilder.setKey(pair.key).setValue(pair.value).build())
                    .collect(Collectors.toList()));
        });

        event.payload.ifPresent(bytes -> eventBuilder.setPayload(ByteString.copyFrom(bytes)));
        return eventBuilder
                .build();
    }

}
