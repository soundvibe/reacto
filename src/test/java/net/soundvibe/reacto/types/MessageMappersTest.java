package net.soundvibe.reacto.types;

import net.soundvibe.reacto.internal.InternalEvent;
import net.soundvibe.reacto.internal.MessageMappers;
import net.soundvibe.reacto.internal.ObjectId;
import net.soundvibe.reacto.internal.proto.Messages;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

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
                .build();

        final Command actual = MessageMappers.toCommand(expected);
        Assert.assertEquals(id, actual.id.toString());
        assertEquals("doSomething", actual.name);
        assertEquals(Optional.empty(), actual.payload);
        assertEquals(Optional.empty(), actual.metaData);
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
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
        assertEquals(ReactiveException.class, actual.error.get().getClass());
        assertEquals(Optional.empty(), actual.metaData);
    }
}
