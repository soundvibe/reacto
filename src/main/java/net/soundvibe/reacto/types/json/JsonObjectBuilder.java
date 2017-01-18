package net.soundvibe.reacto.types.json;

import java.time.Instant;
import java.util.*;
import java.util.function.UnaryOperator;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

/**
 * Helper class for building JsonObject instances.
 */
public final class JsonObjectBuilder {

    private final Map<String, Object> values;

    private JsonObjectBuilder(JsonObject jsonObject) {
        Objects.requireNonNull(jsonObject, "jsonObject cannot be null");
        this.values = new LinkedHashMap<>(jsonObject.values);
    }

    private JsonObjectBuilder(Map<String, Object> map) {
        Objects.requireNonNull(map, "map cannot be null");
        this.values = map;
    }

    public static JsonObjectBuilder create() {
        return new JsonObjectBuilder(new LinkedHashMap<>());
    }

    public static JsonObjectBuilder from(JsonObject jsonObject) {
        return new JsonObjectBuilder(jsonObject);
    }

    public JsonObjectBuilder put(String key, CharSequence value) {
        return putValue(key, value == null ? null : value.toString());
    }

    public JsonObjectBuilder put(String key, JsonObject value) {
        return putValue(key, value);
    }

    public JsonObjectBuilder put(String key, JsonArray value) {
        return putValue(key, value);
    }

    public JsonObjectBuilder putObject(String key, UnaryOperator<JsonObjectBuilder> operator) {
        return put(key, operator.apply(JsonObjectBuilder.create()).build());
    }

    public JsonObjectBuilder putArray(String key, UnaryOperator<JsonArrayBuilder> operator) {
        return put(key, operator.apply(JsonArrayBuilder.create()).build());
    }

    public JsonObjectBuilder put(String key, String value) {
        return putValue(key, value);
    }

    public JsonObjectBuilder put(String key, Integer value) {
        return putValue(key, value);
    }

    public JsonObjectBuilder put(String key, Long value) {
        return putValue(key, value);
    }

    public JsonObjectBuilder put(String key, Float value) {
        return putValue(key, value);
    }

    public JsonObjectBuilder put(String key, Number value) {
        return putValue(key, value);
    }

    public JsonObjectBuilder put(String key, Double value) {
        return putValue(key, value);
    }

    public JsonObjectBuilder put(String key, Boolean value) {
        return putValue(key, value);
    }

    public JsonObjectBuilder put(String key, byte[] value) {
        return putValue(key, value == null ? null : Base64.getEncoder().encodeToString(value));
    }

    public JsonObjectBuilder put(String key, Instant value) {
        return putValue(key, value == null ? null : ISO_INSTANT.format(value));
    }

    public <T extends Enum> JsonObjectBuilder put(String key, T value) {
        return putValue(key, value == null ? null : value.name());
    }

    public JsonObjectBuilder putNull(String key) {
        return putValue(key, null);
    }

    public JsonObjectBuilder merge(JsonObject jsonObject) {
        values.putAll(jsonObject.values);
        return this;
    }

    public JsonObjectBuilder merge(JsonObjectBuilder builder) {
        values.putAll(builder.values);
        return this;
    }

    public JsonObjectBuilder clear() {
        values.clear();
        return this;
    }

    public JsonObjectBuilder remove(String key) {
        values.remove(key);
        return this;
    }

    public JsonObject build() {
        return new JsonObject(values);
    }

    private <T> JsonObjectBuilder putValue(String key, T value) {
        Objects.requireNonNull(key, "key cannot be null");
        values.put(key, value);
        return this;
    }
}
