package net.soundvibe.reacto.types;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Linas on 2017.01.10.
 */
public class TypedCommandTest {

    @Test
    public void shouldCreateWithMetadata() throws Exception {
        final TypedCommand actual = TypedCommand.create(MakeDemo.class, DemoMade.class, MetaData.of("key", "value"));
        assertEquals(DemoMade.class.getName(), actual.eventType());
        assertEquals(MakeDemo.class.getName(), actual.commandType());
        assertEquals("value", actual.get("key"));
    }

    @Test
    public void shouldCreateWithMetadataAndCommand() throws Exception {
        final TypedCommand actual = TypedCommand.create(MakeDemo.class, DemoMade.class, MetaData.of("key", "value"),
                "command".getBytes());
        assertEquals(DemoMade.class.getName(), actual.eventType());
        assertEquals(MakeDemo.class.getName(), actual.commandType());
        assertEquals("value", actual.get("key"));
    }

}