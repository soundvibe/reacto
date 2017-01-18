package net.soundvibe.reacto.types.json;

import java.util.Map;

/**
 * @author OZY on 2017.01.18.
 */
@FunctionalInterface
public interface JsonStringEncoder {

    String encode(Map<String, Object> map) throws Throwable;

    default String encode(JsonObject jsonObject) {
        try {
            return encode(jsonObject.values);
        } catch (Throwable e) {
            throw new JsonMapperException("Error when encoding jsonObject to json string: " + jsonObject, e);
        }
    }

}
