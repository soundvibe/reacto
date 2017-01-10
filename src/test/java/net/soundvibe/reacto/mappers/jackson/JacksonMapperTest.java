package net.soundvibe.reacto.mappers.jackson;

import io.vertx.core.json.Json;
import net.soundvibe.reacto.types.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Linas on 2017.01.10.
 */
public class JacksonMapperTest {

    private final JacksonMapper sut = new JacksonMapper(Json.mapper);

    @Test
    public void shouldMapCommands() throws Exception {
        final JacksonCommand expected = new JacksonCommand("foo");

        final TypedCommand typedCommand = sut.toCommand(expected, JacksonEvent.class);
        final JacksonCommand actual = sut.toGenericCommand(typedCommand, JacksonCommand.class);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldMapEvents() throws Exception {
        final JacksonEvent expected = new JacksonEvent("bar");
        final TypedEvent typedEvent = sut.toEvent(expected);
        final JacksonEvent actual = sut.toGenericEvent(typedEvent, JacksonEvent.class);
        assertEquals(expected, actual);
    }
}