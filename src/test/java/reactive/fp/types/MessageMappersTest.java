package reactive.fp.types;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * @author OZY on 2016.02.05.
 */
public class MessageMappersTest {

    @Test
    public void shouldMapToCommand() throws Exception {

        final Messages.Command expected = Messages.Command.newBuilder()
                .setId("1")
                .setName("doSomething")
                .build();

        final Command actual = MessageMappers.toCommand(expected);
        assertEquals("1", actual.id.toString());
        assertEquals("doSomething", actual.name);
        assertEquals(Optional.empty(), actual.payload);
        assertEquals(Optional.empty(), actual.metaData);
    }
}