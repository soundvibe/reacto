package net.soundvibe.reacto.types.json;

import java.time.Instant;
import java.util.*;
import java.util.function.UnaryOperator;

import static java.time.format.DateTimeFormatter.ISO_INSTANT;

/**
 * @author OZY on 2017.01.18.
 */
public final class JsonArrayBuilder {

    private final List<Object> values;

    private JsonArrayBuilder(List<Object> list) {
        Objects.requireNonNull(list, "list cannot be null");
        this.values = list;
    }

    private JsonArrayBuilder(JsonArray jsonArray) {
        Objects.requireNonNull(jsonArray, "jsonArray cannot be null");
        this.values = new ArrayList<>(jsonArray.values);
    }

    public static JsonArrayBuilder create() {
        return new JsonArrayBuilder(new ArrayList<>());
    }

    public static JsonArrayBuilder from(JsonArray jsonArray) {
        return new JsonArrayBuilder(jsonArray);
    }

    public JsonArray build() {
        return new JsonArray(values);
    }

    public JsonArrayBuilder add(CharSequence value) {
        return addValue(value == null ? null : value.toString());
    }

    public JsonArrayBuilder add(JsonObject value) {
        return addValue(value);
    }

    public JsonArrayBuilder addArray(UnaryOperator<JsonArrayBuilder> operator) {
        return add(operator.apply(JsonArrayBuilder.create()).build());
    }

    public JsonArrayBuilder addObject(UnaryOperator<JsonObjectBuilder> operator) {
        return add(operator.apply(JsonObjectBuilder.create()).build());
    }

    public JsonArrayBuilder add(JsonArray value) {
        return addValue(value);
    }

    public JsonArrayBuilder add(String value) {
        return addValue(value);
    }

    public JsonArrayBuilder add(Integer value) {
        return addValue(value);
    }

    public JsonArrayBuilder add(Long value) {
        return addValue(value);
    }

    public JsonArrayBuilder add(Number value) {
        return addValue(value);
    }

    public JsonArrayBuilder add(Double value) {
        return addValue(value);
    }

    public JsonArrayBuilder add(Boolean value) {
        return addValue(value);
    }

    public JsonArrayBuilder add(byte[] value) {
        return addValue(value == null ? null : Base64.getEncoder().encodeToString(value));
    }

    public JsonArrayBuilder add(Instant value) {
        return addValue(value == null ? null : ISO_INSTANT.format(value));
    }

    public <T extends Enum> JsonArrayBuilder add(T value) {
        return addValue(value == null ? null : value.name());
    }

    public JsonArrayBuilder addNull() {
        return addValue(null);
    }

    public JsonArrayBuilder merge(JsonArray jsonArray) {
        values.addAll(jsonArray.values);
        return this;
    }

    public JsonArrayBuilder merge(JsonArrayBuilder builder) {
        values.addAll(builder.values);
        return this;
    }

    public JsonArrayBuilder remove(int index) {
        values.remove(index);
        return this;
    }

    public JsonArrayBuilder clear() {
        values.clear();
        return this;
    }

    private <T> JsonArrayBuilder addValue(T value) {
        values.add(value);
        return this;
    }
}
