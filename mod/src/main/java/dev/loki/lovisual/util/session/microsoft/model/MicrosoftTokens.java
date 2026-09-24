package dev.loki.lovisual.util.session.microsoft.model;
import dev.loki.lovisual.util.session.microsoft.MicrosoftJson;
import dev.loki.lovisual.util.session.microsoft.auth.MicrosoftAuthException;


import com.google.gson.JsonObject;

public record MicrosoftTokens(String accessToken, String refreshToken) {
    public static MicrosoftTokens fromJson(JsonObject json) {
        try {
            return new MicrosoftTokens(
                    MicrosoftJson.string(json, "access_token"),
                    MicrosoftJson.string(json, "refresh_token")
            );
        } catch (Throwable t) {
            throw new MicrosoftAuthException("Unable to parse Microsoft OAuth token response", t);
        }
    }
}
