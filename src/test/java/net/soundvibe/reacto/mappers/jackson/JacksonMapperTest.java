package net.soundvibe.reacto.mappers.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.types.json.*;
import net.soundvibe.reacto.utils.models.NotDeserializable;
import org.junit.*;

import java.io.UncheckedIOException;
import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @author Linas on 2017.01.10.
 */
public class JacksonMapperTest {

    private final static ObjectMapper json = new ObjectMapper();
    private final JacksonMapper sut = new JacksonMapper(json);

    @BeforeClass
    public static void setUpClass() throws Exception {
        json.registerModule(JacksonMapper.jsonTypesModule());
    }

    @Test
    public void shouldEncodeToJson() throws Exception {
        JsonObject jsonObject = JsonObjectBuilder.create()
                .put("foo", "bar")
                .put("version", 1)
                .build();

        String actual = json.writeValueAsString(jsonObject);
        assertEquals("{\"foo\":\"bar\",\"version\":1}", actual);
    }

    @Test
    public void shouldDecodeFromJson() throws Exception {
        String jsonString = "{\"foo\":\"bar\",\"version\":1}";
        JsonObject expected = JsonObjectBuilder.create()
                .put("foo", "bar")
                .put("version", 1)
                .build();
        JsonObject actual = json.readValue(jsonString, JsonObject.class);
        assertEquals(expected, actual);
    }

    @Test
    public void shouldMapCommands() throws Exception {
        final JacksonCommand expected = new JacksonCommand("foo");

        final TypedCommand typedCommand = sut.toCommand(expected, JacksonEvent.class);
        final JacksonCommand actual = sut.toGenericCommand(typedCommand, JacksonCommand.class);
        assertEquals(expected, actual);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenToGenericEventAndPayloadIsMissing() throws Exception {
        sut.toGenericEvent(Event.create("fpp", MetaData.of("foo", "bar")), JacksonEvent.class);
    }

    @Test(expected = UncheckedIOException.class)
    public void shouldThrowWhenToGenericEvent() throws Exception {
        sut.toGenericEvent(Event.create("fpp", MetaData.of("foo", "bar"), "foo".getBytes()), JacksonEvent.class);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowWhenToGenericCommandAndPayloadIsMissing() throws Exception {
        sut.toGenericCommand(Command.create("fpp", MetaData.of("foo", "bar")), JacksonCommand.class);
    }

    @Test(expected = UncheckedIOException.class)
    public void shouldThrowWhenToGenericCommand() throws Exception {
        sut.toGenericCommand(Command.create("fpp", MetaData.of("foo", "bar"), "foo".getBytes()), JacksonCommand.class);
    }

    @Test
    public void shouldMapEvents() throws Exception {
        final JacksonEvent expected = new JacksonEvent("bar");
        final TypedEvent typedEvent = sut.toEvent(expected);
        final JacksonEvent actual = sut.toGenericEvent(typedEvent, JacksonEvent.class);
        assertEquals(expected, actual);
    }

    private final class IncompatibleWithJackson<T, V> extends Abstract<T> {

        private Map<NotDeserializable, Object> map = new HashMap<>();

        private IncompatibleWithJackson() {
            super(null);
            throw new RuntimeException("error");
        }

        private IncompatibleWithJackson(T data, V v) {
            super(data);
            map.put(new NotDeserializable("ssd"), this);
        }
    }

    private abstract class Abstract<T> {
        private final T data;

        Abstract(T data) {
            this.data = data;
        }
    }
}