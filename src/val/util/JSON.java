package val.util;

import com.google.gson.*;
import val.Constants;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Map;

public final class JSON {

    public static final JsonElement emptyJSON = new JsonObject();

    private JSON() {
    } //never

    public static JsonElement prepareRequest(final JsonObject json) {
        json.addProperty(Constants.PROTOCOL, "B1");
        return json;
    }

    public static JsonElement parse(String jsonString) {
        return parse(new StringReader(jsonString));
    }

    public static JsonElement parse(Reader jsonReader) {
        JsonElement json = new JsonParser().parse(jsonReader);
        if (json.isJsonPrimitive()) {
            throw new JsonParseException("Json is primitive, was probably bad json interpreted as string");
        }
        return json;
    }

    public static JsonElement cloneJson(JsonElement json) {
        return parse(toJsonString(json));
    }

    public static void addAll(JsonObject parent, JsonObject objectToAdd) {
        for (Map.Entry<String, JsonElement> entry : objectToAdd.entrySet()) {
            parent.add(entry.getKey(), entry.getValue());
        }
    }

    public static void writeTo(JsonElement jsonElement, Writer writer) throws IOException {
        writer.write(toJsonString(jsonElement));
        writer.flush();
    }

    public static String toJsonString(JsonElement jsonElement) {
        return jsonElement != null ? jsonElement.toString() : "null";
    }

    public static JsonObject getAsJsonObject(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonObject() ? jsonElement.getAsJsonObject() : new JsonObject();
    }

    public static JsonArray getAsJsonArray(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonArray() ? jsonElement.getAsJsonArray() : new JsonArray();
    }

    public static String getAsString(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonPrimitive() ? jsonElement.getAsString() : null;
    }

    public static long getAsLong(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonPrimitive() ? jsonElement.getAsLong() : 0;
    }

    public static int getAsInt(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonPrimitive() ? jsonElement.getAsInt() : 0;
    }

    public static short getAsShort(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonPrimitive() ? jsonElement.getAsShort() : 0;
    }

    public static byte getAsByte(JsonElement jsonElement) {
        return jsonElement != null && jsonElement.isJsonPrimitive() ? jsonElement.getAsByte() : 0;
    }

    public static boolean getAsBoolean(JsonElement jsonElement) {
        return (jsonElement != null && jsonElement.isJsonPrimitive()) && jsonElement.getAsBoolean();
    }
}
