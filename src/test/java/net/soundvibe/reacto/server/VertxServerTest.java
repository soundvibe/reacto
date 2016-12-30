package net.soundvibe.reacto.server;

import io.vertx.core.json.*;
import org.junit.Test;
import rx.Observable;

import static org.junit.Assert.assertTrue;

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
        final String expected1 = "{\"commands\":[\"bar\",\"foo\"]}";
        final String expected2 = "{\"commands\":[\"foo\",\"bar\"]}";
        assertTrue(actual.equals(expected1) || actual.equals(expected2));
    }
}