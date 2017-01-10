package net.soundvibe.reacto.server;

import io.vertx.core.json.*;
import net.soundvibe.reacto.types.*;
import net.soundvibe.reacto.utils.DemoCommandRegistryMapper;
import org.junit.Test;
import rx.Observable;

import static org.junit.Assert.*;

/**
 * @author OZY on 2016.12.30.
 */
public class VertxServerTest {

    @Test
    public void shouldSerializeCommandsToJson() throws Exception {
        CommandRegistry commandRegistry = CommandRegistry
                .of("foo", command -> Observable.empty())
                .and("bar", command -> Observable.empty());

        final JsonArray array = VertxServer.commandsToJsonArray(commandRegistry);
        final JsonObject jsonObject = new JsonObject().put("commands", array);
        final String actual = jsonObject.encode();
        final String expected1 = "{\"commands\":[{\"commandType\":\"bar\",\"eventType\":\"\"},{\"commandType\":\"foo\",\"eventType\":\"\"}]}";
        final String expected2 = "{\"commands\":[{\"commandType\":\"foo\",\"eventType\":\"\"},{\"commandType\":\"bar\",\"eventType\":\"\"}]}";
        assertTrue("Was not " + expected1 + " or " + expected2 + " but was " + actual,
                actual.equals(expected1) || actual.equals(expected2));
    }

    @Test
    public void shouldSerializeTypedCommandToJson() throws Exception {
        CommandRegistry commandRegistry = CommandRegistry
                .ofTyped(MakeDemo.class, DemoMade.class, makeDemo -> Observable.empty(), new DemoCommandRegistryMapper());

        final JsonArray array = VertxServer.commandsToJsonArray(commandRegistry);
        final JsonObject jsonObject = new JsonObject().put("commands", array);
        final String actual = jsonObject.encode();
        final String expected = "{\"commands\":[{\"commandType\":\"net.soundvibe.reacto.types.MakeDemo\",\"eventType\":\"net.soundvibe.reacto.types.DemoMade\"}]}";
        assertEquals(expected, actual);
    }
}