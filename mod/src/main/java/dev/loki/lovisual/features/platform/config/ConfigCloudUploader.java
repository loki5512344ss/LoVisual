package dev.loki.lovisual.features.platform.config;

import com.google.gson.JsonObject;
import dev.loki.lovisual.config.profile.codec.ConfigProfileType;
import dev.loki.lovisual.config.profile.service.ConfigProfileCollector;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileSnapshot;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.platform.PlatformSession;
import dev.loki.lovisual.features.platform.http.ApiResult;
import dev.loki.lovisual.features.platform.http.PlatformHttpClient;
import dev.loki.lovisual.features.platform.json.ConfigProfileJsonCodec;
import net.minecraft.client.Minecraft;

/**
 * Backs {@code %config save <slot>}: snapshots the current module settings and
 * uploads them to the authenticated account's cloud slot through
 * {@code PUT /configs/{slot}} (gateway forwards it to configs-service, which
 * returns the slot's {@code share_code}). Complements {@link ConfigShareLoader}
 * ({@code %config load <code>}) — the pair is what makes a config portable:
 * upload here yields the code a friend redeems there. Kept out of
 * {@code ConfigCommand} for the same line-budget/package-cohesion reasons.
 */
public final class ConfigCloudUploader {

    private static final ConfigProfileCollector COLLECTOR = new ConfigProfileCollector();

    private ConfigCloudUploader() {
    }

    /**
     * Wraps a snapshot in the {@code {name, data}} body configs-service expects
     * on {@code PUT /configs/{slot}}. Split out from {@link #upload} so it is
     * unit-testable without a live backend or a running client.
     */
    public static JsonObject buildBody(String name, ConfigProfileSnapshot snapshot) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.add("data", ConfigProfileJsonCodec.toJson(snapshot));
        return body;
    }

    public static void upload(int slot, String rawName) {
        if (slot < 1 || slot > 4) {
            CommandOutput.warning("Slot must be 1..4. Usage: %config save <1-4> [name]");
            return;
        }
        if (!PlatformSession.get().isLinked()) {
            CommandOutput.error("Not linked to a platform account. Run %link first.");
            return;
        }
        String name = (rawName == null || rawName.isBlank()) ? ("Slot " + slot) : rawName.trim();
        ConfigProfileSnapshot snapshot = COLLECTOR.collect(ConfigProfileType.MODULES, name);
        CommandOutput.send("Uploading \"" + name + "\" to cloud slot " + slot + "...");
        PlatformHttpClient.put("/configs/" + slot, buildBody(name, snapshot), PlatformSession.get().getDeviceToken())
                .thenAccept(result -> onUploaded(slot, result));
    }

    private static void onUploaded(int slot, ApiResult result) {
        dispatch(() -> {
            if (!result.success() || result.asObject() == null) {
                CommandOutput.error("Cloud upload failed: " + result.error());
                return;
            }
            JsonObject json = result.asObject();
            String shareCode = json.has("share_code") && !json.get("share_code").isJsonNull()
                    ? json.get("share_code").getAsString() : "?";
            CommandOutput.success("Saved slot " + slot + ". Share code: " + shareCode
                    + " (friend uses %config load " + shareCode + ").");
        });
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
