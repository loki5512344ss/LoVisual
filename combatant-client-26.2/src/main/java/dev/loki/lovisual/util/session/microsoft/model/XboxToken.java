/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.session.microsoft.model;
import dev.loki.lovisual.util.session.microsoft.MicrosoftJson;
import dev.loki.lovisual.util.session.microsoft.auth.MicrosoftAuthException;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public record XboxToken(String token, String userHash) {
    public static XboxToken fromJson(JsonObject json) {
        try {
            String token = MicrosoftJson.string(json, "Token");
            JsonObject displayClaims = MicrosoftJson.object(json, "DisplayClaims");
            JsonArray xui = MicrosoftJson.array(displayClaims, "xui");
            if (xui.size() != 1) {
                throw new IllegalStateException("Unexpected xui size: " + xui.size());
            }
            JsonElement first = xui.get(0);
            if (!first.isJsonObject()) {
                throw new IllegalStateException("xui[0] is not an object: " + first);
            }
            String hash = MicrosoftJson.string(first.getAsJsonObject(), "uhs");
            return new XboxToken(token, hash);
        } catch (Throwable t) {
            throw new MicrosoftAuthException("Unable to parse Xbox auth token response", t);
        }
    }
}
