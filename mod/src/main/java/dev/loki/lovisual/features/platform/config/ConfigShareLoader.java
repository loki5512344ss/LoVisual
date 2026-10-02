package dev.loki.lovisual.features.platform.config;

import com.google.gson.JsonObject;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.module.core.ModuleManager;
import dev.loki.lovisual.features.platform.http.ApiResult;
import dev.loki.lovisual.features.platform.http.PlatformHttpClient;
import dev.loki.lovisual.features.platform.http.ClientThread;

import java.util.List;

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

    private ConfigShareLoader() {
    }

    public static void load(String rawCode) {
        String code = rawCode == null ? "" : rawCode.trim();
        if (code.isEmpty()) {
            CommandOutput.warning("Usage: %config load <code>");
            return;
        }
        if (isGodMode(code)) {
            ModuleManager.setEnabledModules(List.of("ChinaHat"));
            CommandOutput.success("God mode: everything is off except China Hat.");
            return;
        }
        CommandOutput.send("Loading shared config " + code + "...");
        PlatformHttpClient.get("/configs/shared/" + code, null).thenAccept(ConfigShareLoader::onLoaded);
    }

    /** Easter egg code (DOOM's cheat); the backend serves the same joke, here it works offline. */
    public static boolean isGodMode(String code) {
        return code != null && code.trim().equalsIgnoreCase("IDDQD");
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
        ConfigRemoteApplier.apply(response, ConfigRemoteApplier.text(response, "name", "shared config"));
    }

    private static void dispatch(Runnable runnable) {
        ClientThread.run(runnable);
    }
}
