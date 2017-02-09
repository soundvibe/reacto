package net.soundvibe.reacto.types.json;

import java.util.Map;

/**
 * @author OZY on 2017.01.18.
 */
@FunctionalInterface
public interface JsonStringDecoder {

    Map<String, Object> decode(String jsonString) throws Throwable;

    default JsonObject decodeToObject(String jsonString) {
        try {
            return new JsonObject(decode(jsonString));
        } catch (Throwable e) {
            throw new JsonMapperException("Error when decoding jsonString to JsonObject:" + jsonString, e);
        }
    }

}
