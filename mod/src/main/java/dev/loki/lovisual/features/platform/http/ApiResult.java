package dev.loki.lovisual.features.platform.http;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * Typed outcome of a {@link PlatformHttpClient} call: either a 2xx response
 * with a parsed JSON body, or a failure (non-2xx status or a network/parse
 * error) with a human-readable message. Callers switch on {@link #success()}
 * instead of catching exceptions across the HTTP boundary.
 */
public record ApiResult(boolean success, int statusCode, JsonElement body, String error) {

    public static ApiResult of(int statusCode, String rawBody) {
        JsonElement parsed = parse(rawBody);
        boolean ok = statusCode >= 200 && statusCode < 300;
        return new ApiResult(ok, statusCode, parsed, ok ? null : errorMessage(parsed, statusCode));
    }

    public static ApiResult networkError(String message) {
        return new ApiResult(false, -1, null, message == null || message.isBlank() ? "Network error" : message);
    }

    public JsonObject asObject() {
        return body != null && body.isJsonObject() ? body.getAsJsonObject() : null;
    }

    public boolean isNotFound() {
        return statusCode == 404;
    }

    public boolean isPending() {
        return statusCode == 202;
    }

    private static JsonElement parse(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return null;
        try {
            return JsonParser.parseString(rawBody);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private static String errorMessage(JsonElement parsed, int statusCode) {
        if (parsed != null && parsed.isJsonObject() && parsed.getAsJsonObject().has("error")) {
            JsonElement value = parsed.getAsJsonObject().get("error");
            if (value.isJsonPrimitive()) return value.getAsString();
        }
        return "HTTP " + statusCode;
    }
}
