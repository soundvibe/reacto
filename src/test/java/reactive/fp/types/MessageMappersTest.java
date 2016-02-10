package reactive.fp.types;

import org.junit.Test;
import reactive.fp.internal.MessageMappers;
import reactive.fp.internal.Messages;
import reactive.fp.internal.ObjectId;

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
        assertEquals(id, actual.id.toString());
        assertEquals("doSomething", actual.name);
        assertEquals(Optional.empty(), actual.payload);
        assertEquals(Optional.empty(), actual.metaData);
    }
}
