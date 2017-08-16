package net.soundvibe.reacto.types;

import com.google.protobuf.ByteString;
import net.soundvibe.reacto.errors.ReactiveException;
import net.soundvibe.reacto.internal.*;
import net.soundvibe.reacto.internal.proto.Messages;
import net.soundvibe.reacto.mappers.Mappers;
import org.junit.*;

import java.util.Optional;

import static net.soundvibe.reacto.internal.InternalEvent.COMMAND_ID;
import static org.junit.Assert.assertEquals;

/**
 * @author OZY on 2016.02.05.
 */
public class MessageMappersTest {

    @Test
    public void shouldMapToCommand() throws Exception {
        final String id = ObjectId.get().toString();
        final Messages.Command expected = Messages.Command.newBuilder()
                .setId(id)
                .setName("doSomething")
                .addMetadata(Messages.Metadata.newBuilder()
                        .setKey("key1").setValue("value1")
                        .build())
                .build();

        final Command actual = MessageMappers.toCommand(expected);
        Assert.assertEquals(id, actual.id.toString());
        assertEquals("doSomething", actual.name);
        assertEquals(Optional.empty(), actual.payload);
        assertEquals(Optional.of(MetaData.of("key1", "value1")), actual.metaData);
    }

    @Test
    public void shouldMapToEventContainingException() throws Exception {
        final Messages.Event expected = Messages.Event.newBuilder()
                .setEventType(Messages.EventType.ERROR)
                .setId("1")
                .setName("foo")
                .setError(Messages.Error.newBuilder()
                        .setClassName("fooClass")
                        .setErrorMessage("error")
                        .setStackTrace("")
                        .build())
                .build();

        final InternalEvent actual = MessageMappers.toInternalEvent(expected);
        assertEquals("foo", actual.name);
        assertEquals(ReactiveException.class, actual.error.orElseGet(NullPointerException::new).getClass());
        assertEquals(Optional.empty(), actual.metaData);
    }

    @Test
    public void shouldMapExceptionFromPayload() throws Exception {
        final Messages.Event expected = Messages.Event.newBuilder()
                .setEventType(Messages.EventType.ERROR)
                .setId("1")
                .setName("foo")
                .setPayload(ByteString.copyFrom(Mappers.exceptionToBytes(new IllegalArgumentException("Error")).orElse(new byte[0])))
                .build();

        final InternalEvent actual = MessageMappers.toInternalEvent(expected);
        assertEquals("foo", actual.name);
        assertEquals(IllegalArgumentException.class, actual.error.orElseGet(NullPointerException::new).getClass());
    }

    @Test
    public void shouldMapToProtoBufEvent() throws Exception {
        InternalEvent internalEvent = InternalEvent.onError(new IllegalStateException("Error"), "cmdId");

        Messages.Event actual = MessageMappers.toProtoBufEvent(internalEvent);
        assertEquals(Messages.EventType.ERROR, actual.getEventType());
        assertEquals(IllegalStateException.class.getName(), actual.getError().getClassName());
        assertEquals(Messages.Metadata.newBuilder()
                .setKey(COMMAND_ID).setValue("cmdId")
                .build(), actual.getMetadataList().get(0));
    }

    @Test
    public void shouldMapToProtoBufCommand() throws Exception {
        Command command = Command.create("foo", MetaData.of("key1", "value1"));

        Messages.Command actual = MessageMappers.toProtoBufCommand(command);

        assertEquals(command.name, actual.getName());
        assertEquals(command.id.toString(), actual.getId());
        assertEquals(Messages.Metadata.newBuilder()
                .setKey("key1").setValue("value1")
                .build(), actual.getMetadataList().get(0));
    }
}
