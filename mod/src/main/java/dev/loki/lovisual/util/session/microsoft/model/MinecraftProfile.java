/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.session.microsoft.model;
import dev.loki.lovisual.util.session.microsoft.MicrosoftJson;
import dev.loki.lovisual.util.session.microsoft.auth.MicrosoftAuthException;


import com.google.gson.JsonObject;

import java.util.UUID;
import java.util.regex.Pattern;

public record MinecraftProfile(UUID uuid, String name) {
    private static final Pattern DASHLESS_UUID = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

    public static MinecraftProfile fromJson(JsonObject json) {
        try {
            String id = MicrosoftJson.string(json, "id");
            if (!DASHLESS_UUID.matcher(id).matches()) {
                throw new IllegalStateException("Invalid Minecraft profile UUID: " + id);
            }
            String dashed = DASHLESS_UUID.matcher(id).replaceAll("$1-$2-$3-$4-$5");
            return new MinecraftProfile(UUID.fromString(dashed), MicrosoftJson.string(json, "name"));
        } catch (Throwable t) {
            throw new MicrosoftAuthException("Unable to parse Minecraft profile response", t);
        }
    }
}
