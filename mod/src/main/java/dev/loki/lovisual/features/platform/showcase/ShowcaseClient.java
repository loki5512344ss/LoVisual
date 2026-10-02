package dev.loki.lovisual.features.platform.showcase;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.loki.lovisual.features.command.util.CommandOutput;
import dev.loki.lovisual.features.platform.PlatformSession;
import dev.loki.lovisual.features.platform.config.ConfigRemoteApplier;
import dev.loki.lovisual.features.platform.http.ApiResult;
import dev.loki.lovisual.features.platform.http.ClientThread;
import dev.loki.lovisual.features.platform.http.PlatformHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Talks to the public config showcase: browse ({@code GET /showcase}, no
 * auth), try a listing without saving it, copy it into a cloud slot, and
 * publish/unpublish one of the account's own slots. Remembers the ids of the
 * last browsed page so commands can refer to a listing by its number.
 */
public final class ShowcaseClient {

    private static volatile List<String> lastPageIds = List.of();

    private ShowcaseClient() {
    }

    public record Page(List<JsonObject> items, boolean hasMore) {
    }

    public static void browse(String sort, int page) {
        fetchPage(sort, page, result -> {
            if (result == null) {
                CommandOutput.error("Failed to load the showcase.");
                return;
            }
            if (result.items().isEmpty()) {
                CommandOutput.send("The showcase is empty on page " + page + ".");
            }
            int index = 1;
            for (JsonObject listing : result.items()) {
                CommandOutput.send(ShowcaseFormat.listingLine(index++, listing));
            }
            if (result.hasMore()) {
                CommandOutput.send("More on the next page: %showcase <new|popular> " + (page + 1));
            }
            if (!result.items().isEmpty()) {
                CommandOutput.send("Try one: %showcase load <number>, keep one: %showcase copy <number> <1-4>");
            }
        });
    }

    /** Loads one page and remembers its ids for number references; {@code onDone} gets {@code null} on failure. */
    public static void fetchPage(String sort, int page, Consumer<Page> onDone) {
        PlatformHttpClient.get("/showcase?sort=" + sort + "&page=" + page, null)
                .thenAccept(result -> ClientThread.run(() -> onDone.accept(parsePage(result))));
    }

    private static Page parsePage(ApiResult result) {
        JsonObject body = result.asObject();
        if (!result.success() || body == null || !body.has("items") || !body.get("items").isJsonArray()) {
            return null;
        }
        List<JsonObject> items = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (JsonElement item : body.getAsJsonArray("items")) {
            items.add(item.getAsJsonObject());
            ids.add(item.getAsJsonObject().get("id").getAsString());
        }
        lastPageIds = List.copyOf(ids);
        return new Page(items, body.has("has_more") && body.get("has_more").getAsBoolean());
    }

    public static void apply(String ref) {
        String id = ShowcaseFormat.resolveId(ref, lastPageIds);
        if (id == null) {
            CommandOutput.warning("Unknown listing. Browse with %showcase first, then use its number or a full id.");
            return;
        }
        PlatformHttpClient.get("/showcase/" + id, null).thenAccept(result -> ClientThread.run(() -> {
            if (result.isNotFound()) {
                CommandOutput.error("That listing no longer exists.");
            } else if (!result.success() || result.asObject() == null) {
                CommandOutput.error("Failed to load listing: " + result.error());
            } else {
                JsonObject body = result.asObject();
                ConfigRemoteApplier.apply(body, ConfigRemoteApplier.text(body, "title", "listing"));
            }
        }));
    }

    public static void copy(String ref, int slot) {
        String id = ShowcaseFormat.resolveId(ref, lastPageIds);
        if (id == null || slot < 1 || slot > 4) {
            CommandOutput.warning("Usage: %showcase copy <number|id> <1-4>");
            return;
        }
        if (!requireLink()) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("slot", slot);
        PlatformHttpClient.post("/showcase/" + id + "/copy", body, token()).thenAccept(result -> ClientThread.run(() -> {
            if (result.success()) {
                CommandOutput.success("Copied into cloud slot " + slot + ". Use %config pull " + slot + " to apply it.");
            } else if (result.statusCode() == 409) {
                CommandOutput.error("Cloud slot " + slot + " is not empty. Pick an empty slot (see %config slots).");
            } else {
                CommandOutput.error("Copy failed: " + result.error());
            }
        }));
    }

    public static void publish(int slot, String title) {
        if (slot < 1 || slot > 4 || title == null || title.isBlank()) {
            CommandOutput.warning("Usage: %config publish <1-4> <title>");
            return;
        }
        if (!requireLink()) {
            return;
        }
        JsonObject body = new JsonObject();
        body.addProperty("title", title.trim());
        PlatformHttpClient.post("/configs/" + slot + "/publish", body, token())
                .thenAccept(result -> report(result, "Published slot " + slot + " to the showcase.", "Publish failed: "));
    }

    public static void unpublish(int slot) {
        if (slot < 1 || slot > 4) {
            CommandOutput.warning("Usage: %config unpublish <1-4>");
            return;
        }
        if (requireLink()) {
            PlatformHttpClient.delete("/configs/" + slot + "/publish", token())
                    .thenAccept(result -> report(result, "Removed slot " + slot + " from the showcase.", "Unpublish failed: "));
        }
    }

    private static void report(ApiResult result, String okMessage, String failPrefix) {
        ClientThread.run(() -> {
            if (result.success()) {
                CommandOutput.success(okMessage);
            } else {
                CommandOutput.error(failPrefix + result.error());
            }
        });
    }

    private static boolean requireLink() {
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
