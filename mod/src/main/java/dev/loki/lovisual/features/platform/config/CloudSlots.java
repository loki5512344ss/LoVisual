package dev.loki.lovisual.features.platform.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.platform.PlatformSession;
import dev.loki.lovisual.features.platform.http.ClientThread;
import dev.loki.lovisual.features.platform.http.PlatformHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Reads the linked account's four cloud slots: {@code %config slots} lists
 * them ({@code GET /configs}), {@code %config pull <1-4>} applies one
 * ({@code GET /configs/{slot}}). The write side is {@link ConfigCloudUploader}.
 */
public final class CloudSlots {

    private CloudSlots() {
    }

    /** One chat line per slot summary, e.g. {@code 2. My Build [published] code ABCD2345}. */
    public static String slotLine(JsonObject slot) {
        String published = slot.has("published") && slot.get("published").getAsBoolean() ? " [published]" : "";
        return slot.get("slot").getAsInt() + ". " + ConfigRemoteApplier.text(slot, "name", "?") + published
                + " code " + ConfigRemoteApplier.text(slot, "share_code", "?");
    }

    public static void list() {
        if (!requireLink()) {
            return;
        }
        fetch(slots -> {
            if (slots == null) {
                CommandOutput.error("Failed to list cloud slots.");
            } else if (slots.isEmpty()) {
                CommandOutput.send("All 4 cloud slots are empty. Use %config save <1-4> [name].");
            } else {
                slots.forEach(slot -> CommandOutput.send(slotLine(slot)));
            }
        });
    }

    /** Loads the slot summaries; {@code onDone} runs on the client thread with {@code null} on failure. */
    public static void fetch(Consumer<List<JsonObject>> onDone) {
        PlatformHttpClient.get("/configs", token()).thenAccept(result -> ClientThread.run(() -> {
            if (!result.success() || result.body() == null || !result.body().isJsonArray()) {
                onDone.accept(null);
                return;
            }
            List<JsonObject> slots = new ArrayList<>();
            for (JsonElement slot : result.body().getAsJsonArray()) {
                slots.add(slot.getAsJsonObject());
            }
            onDone.accept(slots);
        }));
    }

    public static void pull(int slot) {
        if (slot < 1 || slot > 4) {
            CommandOutput.warning("Slot must be 1..4. Usage: %config pull <1-4>");
            return;
        }
        if (!requireLink()) {
            return;
        }
        PlatformHttpClient.get("/configs/" + slot, token()).thenAccept(result -> ClientThread.run(() -> {
            if (result.isNotFound()) {
                CommandOutput.warning("Cloud slot " + slot + " is empty.");
            } else if (!result.success() || result.asObject() == null) {
                CommandOutput.error("Failed to load slot " + slot + ": " + result.error());
            } else {
                JsonObject body = result.asObject();
                ConfigRemoteApplier.apply(body, ConfigRemoteApplier.text(body, "name", "Slot " + slot));
            }
        }));
    }

    public static boolean requireLink() {
        if (PlatformSession.get().isLinked()) {
            return true;
        }
        CommandOutput.error("Not linked to a platform account. Run %link first.");
        return false;
    }

    private static String token() {
        return PlatformSession.get().getDeviceToken();
    }
}
