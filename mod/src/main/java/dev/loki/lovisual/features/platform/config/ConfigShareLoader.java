package dev.loki.lovisual.features.platform.config;

import com.google.gson.JsonObject;
import dev.loki.lovisual.config.profile.service.ConfigProfileApplier;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileSnapshot;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.platform.http.ApiResult;
import dev.loki.lovisual.features.platform.http.PlatformHttpClient;
import dev.loki.lovisual.features.platform.json.ConfigProfileJsonCodec;
import net.minecraft.client.Minecraft;

/**
 * Backs {@code %config load <code>}: fetches a publicly shared config from
 * {@code GET /configs/shared/{code}} (no auth required per
 * {@code configs-service/PLAN.md}), decodes its {@code data} field into a
 * {@link ConfigProfileSnapshot} via {@link ConfigProfileJsonCodec}, and
 * applies it through the existing {@link ConfigProfileApplier} rather than a
 * second "apply settings" code path. Kept out of
 * {@code features.command.impl.client.ConfigCommand} to respect both that
 * file's line budget and the 4-files-per-folder rule for its package.
 */
public final class ConfigShareLoader {

    private static final ConfigProfileApplier APPLIER = new ConfigProfileApplier();

    private ConfigShareLoader() {
    }

    public static void load(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.isEmpty()) {
            CommandOutput.warning("Usage: %config load <code>");
            return;
        }
        CommandOutput.send("Loading shared config " + code + "...");
        PlatformHttpClient.get("/configs/shared/" + code, null).thenAccept(ConfigShareLoader::onLoaded);
    }

    private static void onLoaded(ApiResult result) {
        dispatch(() -> {
            if (result.isNotFound()) {
                CommandOutput.error("No shared config found for that code.");
                return;
            }
            if (!result.success() || result.asObject() == null) {
                CommandOutput.error("Failed to load shared config: " + result.error());
                return;
            }
            applyShared(result.asObject());
        });
    }

    private static void applyShared(JsonObject response) {
        if (!response.has("data") || !response.get("data").isJsonObject()) {
            CommandOutput.error("Shared config has no usable data.");
            return;
        }
        String name = response.has("name") && !response.get("name").isJsonNull()
                ? response.get("name").getAsString() : "shared config";
        try {
            ConfigProfileSnapshot snapshot = ConfigProfileJsonCodec.fromJson(response.getAsJsonObject("data"));
            ConfigProfileApplier.ApplyResult applied = APPLIER.apply(snapshot);
            CommandOutput.success("Applied \"" + name + "\" (" + applied.appliedOwners() + " settings groups, "
                    + applied.missingOwners() + " missing).");
        } catch (RuntimeException e) {
            CommandOutput.error("Could not decode shared config: " + e.getMessage());
        }
    }

    private static void dispatch(Runnable runnable) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.isSameThread()) {
            runnable.run();
        } else {
            mc.execute(runnable);
        }
    }
}
