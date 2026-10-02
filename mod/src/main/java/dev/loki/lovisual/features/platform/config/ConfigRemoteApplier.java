package dev.loki.lovisual.features.platform.config;

import com.google.gson.JsonObject;
import dev.loki.lovisual.config.profile.service.ConfigProfileApplier;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileSnapshot;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.platform.json.ConfigProfileJsonCodec;

/**
 * Applies a config body fetched from the backend (share code, cloud slot or
 * showcase listing: all carry the same {@code data} profile tree) through the
 * existing {@link ConfigProfileApplier}, so there is a single "apply remote
 * settings" path. Must be called on the client thread.
 */
public final class ConfigRemoteApplier {

    private static final ConfigProfileApplier APPLIER = new ConfigProfileApplier();

    private ConfigRemoteApplier() {
    }

    public static void apply(JsonObject response, String displayName) {
        if (!response.has("data") || !response.get("data").isJsonObject()) {
            CommandOutput.error("Config has no usable data.");
            return;
        }
        try {
            ConfigProfileSnapshot snapshot = ConfigProfileJsonCodec.fromJson(response.getAsJsonObject("data"));
            ConfigProfileApplier.ApplyResult applied = APPLIER.apply(snapshot);
            CommandOutput.success("Applied \"" + displayName + "\" (" + applied.appliedOwners()
                    + " settings groups, " + applied.missingOwners() + " missing).");
        } catch (RuntimeException e) {
            CommandOutput.error("Could not decode config: " + e.getMessage());
        }
    }

    public static String text(JsonObject object, String key, String fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : fallback;
    }
}
