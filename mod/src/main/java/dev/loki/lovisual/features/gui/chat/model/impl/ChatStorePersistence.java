package dev.loki.lovisual.features.gui.chat.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.text.format.TextJsonUtil;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatHoverCache;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Async Gson persistence engine for {@link BetterChatStoreManager}: revision-gated save scheduling,
 * synchronous flush-on-shutdown, JSON (de)serialization and the serialized-line cache. Extracted so
 * the manager facade keeps only the public API and active-store state.
 */
final class ChatStorePersistence {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final Map<String, CompletableFuture<?>> SAVE_JOBS = new HashMap<>();
    private static final Map<String, CompletableFuture<?>> CACHE_SAVE_JOBS = new HashMap<>();
    private static final Map<String, Long> LAST_SAVED_REVISIONS = new HashMap<>();
    private static final Map<String, Long> REQUESTED_SAVE_REVISIONS = new HashMap<>();
    private static final Map<ChatLine, String> SERIALIZED_LINE_CACHE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private ChatStorePersistence() {
    }

    static BetterChatStore loadSync(String key, BetterChatHoverCache cache) {
        File f = ChatStoreKeys.fileFor(key);
        if (!f.exists()) return new BetterChatStore();
        try (FileReader reader = new FileReader(f, StandardCharsets.UTF_8)) {
            JsonArray arr = GSON.fromJson(reader, JsonArray.class);
            BetterChatStore store = new BetterChatStore();
            if (arr != null) {
                for (var el : arr) {
                    if (!el.isJsonObject()) continue;
                    JsonObject obj = el.getAsJsonObject();
                    String json = obj.has("text") ? obj.get("text").getAsString() : null;
                    long ts = obj.has("time") ? obj.get("time").getAsLong() : System.currentTimeMillis();
                    if (json != null) {
                        var txt = TextJsonUtil.fromJson(json);
                        if (txt != null) {
                            store.add(txt, ts);
                            ChatHoverCapture.captureHovers(txt, cache);
                        }
                    }
                }
            }
            DebugLog.info("[BetterChat] Loaded %d messages for %s", store.snapshot().size(), key);
            return store;
        } catch (Exception e) {
            DebugLog.error("BetterChat load failed for %s", e, key);
            return new BetterChatStore();
        }
    }

    static void scheduleSave(String key, BetterChatStore store) {
        if (key == null || store == null) return;
        long revision = store.revision();
        synchronized (ChatStorePersistence.class) {
            long lastSaved = LAST_SAVED_REVISIONS.getOrDefault(key, -1L);
            if (revision <= lastSaved) {
                return;
            }
            REQUESTED_SAVE_REVISIONS.put(key, revision);
            CompletableFuture<?> inFlight = SAVE_JOBS.get(key);
            if (inFlight != null && !inFlight.isDone()) {
                return;
            }
            SAVE_JOBS.put(key, CompletableFuture.runAsync(() -> drainSaveQueue(key, store)));
        }
    }

    static void scheduleCacheSave(String key, BetterChatHoverCache cache) {
        if (key == null || cache == null) return;
        CACHE_SAVE_JOBS.compute(key, (k, prev) -> {
            CompletableFuture<?> base = prev == null
                    ? CompletableFuture.completedFuture(null)
                    : prev.exceptionally(ex -> null);
            return base.thenRunAsync(() -> cache.save(k));
        });
    }

    /**
     * Blocks until all pending async store/cache save jobs have settled.
     */
    static void awaitPending() {
        for (CompletableFuture<?> f : new ArrayList<>(SAVE_JOBS.values())) {
            try {
                f.join();
            } catch (Throwable ignored) {
            }
        }
        for (CompletableFuture<?> f : new ArrayList<>(CACHE_SAVE_JOBS.values())) {
            try {
                f.join();
            } catch (Throwable ignored) {
            }
        }
    }

    static void saveSync(String key, BetterChatStore store) {
        try {
            SavePayload payload = captureSavePayload(store);
            File f = ChatStoreKeys.fileFor(key);
            f.getParentFile().mkdirs();
            JsonArray arr = new JsonArray();
            for (ChatLine line : payload.lines()) {
                JsonObject obj = new JsonObject();
                String json = serializedText(line);
                obj.addProperty("text", json);
                obj.addProperty("time", line.timestampMs());
                arr.add(obj);
            }
            try (FileWriter writer = new FileWriter(f, StandardCharsets.UTF_8)) {
                GSON.toJson(arr, writer);
            }
            synchronized (ChatStorePersistence.class) {
                LAST_SAVED_REVISIONS.put(key, payload.revision());
            }
        } catch (Exception e) {
            DebugLog.error("BetterChat save failed for %s", e, key);
        }
    }

    private static void drainSaveQueue(String key, BetterChatStore store) {
        try {
            while (true) {
                long requested;
                long saved;
                synchronized (ChatStorePersistence.class) {
                    requested = REQUESTED_SAVE_REVISIONS.getOrDefault(key, -1L);
                    saved = LAST_SAVED_REVISIONS.getOrDefault(key, -1L);
                    if (requested <= saved) {
                        SAVE_JOBS.remove(key);
                        return;
                    }
                }
                saveSync(key, store);
            }
        } finally {
            synchronized (ChatStorePersistence.class) {
                CompletableFuture<?> current = SAVE_JOBS.get(key);
                if (current != null && current.isDone()) {
                    SAVE_JOBS.remove(key);
                }
            }
        }
    }

    private static SavePayload captureSavePayload(BetterChatStore store) {
        synchronized (store) {
            return new SavePayload(store.revision(), store.snapshot());
        }
    }

    private static String serializedText(ChatLine line) {
        String cached = SERIALIZED_LINE_CACHE.get(line);
        if (cached != null) {
            return cached;
        }
        String created = TextJsonUtil.toJson(line.text());
        SERIALIZED_LINE_CACHE.put(line, created);
        return created;
    }

    private record SavePayload(long revision, List<ChatLine> lines) {
    }
}
