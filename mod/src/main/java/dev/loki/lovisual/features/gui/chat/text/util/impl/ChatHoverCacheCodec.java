package dev.loki.lovisual.features.gui.chat.text.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.config.io.ConfigPaths;
import dev.loki.lovisual.util.logging.DebugLog;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Disk (de)serialization, registry-ops ItemStack codec and display-name index construction for
 * {@link BetterChatHoverCache}. Extracted so the cache class keeps its public put/get/find API and
 * per-instance maps; operates on the cache's package-private fields (same package).
 */
final class ChatHoverCacheCodec {
    static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final int MAX_COMP_JSON = 20000;

    private static volatile HolderLookup.Provider cachedLookup;
    private static volatile RegistryOps<JsonElement> cachedOps;

    private ChatHoverCacheCodec() {
    }

    static void populate(BetterChatHoverCache cache, String key) {
        File f = fileFor(key);
        if (!f.exists()) return;
        try (FileReader r = new FileReader(f, StandardCharsets.UTF_8)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            cache.loadedFromDisk = true;
            if (obj != null && obj.has("items")) {
                JsonArray arr = obj.getAsJsonArray("items");
                arr.forEach(el -> {
                    if (!el.isJsonObject()) return;
                    JsonObject o = el.getAsJsonObject();
                    String keyId = o.has("k") ? o.get("k").getAsString() : null;
                    String id = o.has("id") ? o.get("id").getAsString() : null;
                    String comp = o.has("comp") ? o.get("comp").getAsString() : null;
                    if (keyId != null && id != null && comp != null) {
                        cache.itemCache.put(keyId, new BetterChatHoverCache.ItemEntry(id, comp));
                        cache.loadedItemKeys.add(keyId);
                    }
                });
            }
            if (obj != null && obj.has("entities")) {
                JsonArray arr = obj.getAsJsonArray("entities");
                arr.forEach(el -> {
                    if (!el.isJsonObject()) return;
                    JsonObject o = el.getAsJsonObject();
                    String keyId = o.has("k") ? o.get("k").getAsString() : null;
                    String name = o.has("name") ? o.get("name").getAsString() : "";
                    String type = o.has("type") ? o.get("type").getAsString() : "";
                    String uuid = o.has("uuid") ? o.get("uuid").getAsString() : null;
                    if (keyId != null && uuid != null && !name.isEmpty()) {
                        try {
                            Identifier tid = Identifier.tryParse(type);
                            if (tid != null) {
                                var et = BuiltInRegistries.ENTITY_TYPE.getValue(tid);
                                if (et != net.minecraft.world.entity.EntityTypes.PLAYER) {
                                    String def = et.getDescription().getString().trim();
                                    if (!def.isEmpty() && def.equalsIgnoreCase(name.trim())) {
                                        return;
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                        cache.entityCache.put(keyId, new BetterChatHoverCache.EntityEntry(name, type, uuid));
                        cache.loadedEntityKeys.add(keyId);
                    }
                });
            }
        } catch (Exception e) {
            DebugLog.error("BetterChatHoverCache load failed for %s", e, key);
        }
    }

    static void write(BetterChatHoverCache cache, String key) {
        try {
            File f = fileFor(key);
            f.getParentFile().mkdirs();
            JsonObject root = new JsonObject();
            JsonArray items = new JsonArray();
            for (var e : cache.itemCache.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("k", e.getKey());
                o.addProperty("id", e.getValue().getId());
                o.addProperty("comp", e.getValue().componentsJson());
                items.add(o);
            }
            JsonArray ents = new JsonArray();
            for (var e : cache.entityCache.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("k", e.getKey());
                o.addProperty("name", e.getValue().name());
                o.addProperty("type", e.getValue().typeId());
                o.addProperty("uuid", e.getValue().uuid());
                ents.add(o);
            }
            root.add("items", items);
            root.add("entities", ents);
            try (FileWriter w = new FileWriter(f, StandardCharsets.UTF_8)) {
                GSON.toJson(root, w);
            }
        } catch (Exception e) {
            DebugLog.error("BetterChatHoverCache save failed for %s", e, key);
        }
    }

    static String normalizeLookup(String raw) {
        return raw == null ? "" : raw.trim().toLowerCase();
    }

    static String encodeComponents(ItemStack stack) {
        RegistryOps<JsonElement> ops = ops();
        if (ops == null) return null;
        var encoded = ItemStack.CODEC.encodeStart(ops, stack.copy());
        var res = encoded.result();
        if (res.isEmpty()) return null;
        String compJson = res.get().toString();
        if (compJson.length() > MAX_COMP_JSON) return null;
        return compJson;
    }

    static ItemStack decodeItem(String componentsJson) {
        RegistryOps<JsonElement> ops = ops();
        if (ops == null) return null;
        try {
            var parsed = ItemStack.CODEC.parse(ops, JsonParser.parseString(componentsJson));
            if (parsed.result().isPresent()) {
                return parsed.result().get();
            }
        } catch (Exception ex) {
            DebugLog.error("BetterChatHoverCache getItem decode failed", ex);
        }
        return null;
    }

    static void ensureItemLookupIndex(BetterChatHoverCache cache) {
        if (cache.itemLookupIndexBuilt) return;
        cache.itemLookupByName.clear();
        for (var kv : cache.itemCache.entrySet()) {
            String key = kv.getKey();
            ItemStack stack = cache.getItem(key);
            if (stack.isEmpty()) continue;
            String normalized = normalizeLookup(stack.getHoverName().getString());
            if (normalized.isEmpty() || cache.itemLookupByName.containsKey(normalized)) continue;
            boolean loaded = cache.loadedFromDisk && cache.loadedItemKeys.contains(key);
            cache.itemLookupByName.put(normalized, new BetterChatHoverCache.ItemLookup(stack.copy(), loaded));
        }
        cache.itemLookupIndexBuilt = true;
    }

    static void ensureEntityLookupIndex(BetterChatHoverCache cache) {
        if (cache.entityLookupIndexBuilt) return;
        cache.entityLookupByName.clear();
        for (var kv : cache.entityCache.entrySet()) {
            String key = kv.getKey();
            BetterChatHoverCache.EntityEntry entry = kv.getValue();
            String normalized = normalizeLookup(entry.name());
            if (normalized.isEmpty() || cache.entityLookupByName.containsKey(normalized)) continue;
            try {
                HoverEvent.EntityTooltipInfo content = new HoverEvent.EntityTooltipInfo(
                        BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse(entry.typeId())),
                        UUID.fromString(entry.uuid()),
                        entry.name().isEmpty() ? null : Component.literal(entry.name())
                );
                boolean loaded = cache.loadedFromDisk && cache.loadedEntityKeys.contains(key);
                cache.entityLookupByName.put(normalized, new BetterChatHoverCache.EntityLookup(content, loaded));
            } catch (Exception ignored) {
            }
        }
        cache.entityLookupIndexBuilt = true;
    }

    private static HolderLookup.Provider lookup() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return null;
        ClientPacketListener net = mc.getConnection();
        if (net != null) return net.registryAccess();
        if (mc.level != null) return mc.level.registryAccess();
        return null;
    }

    private static RegistryOps<JsonElement> ops() {
        HolderLookup.Provider lookup = lookup();
        if (lookup == null) {
            return null;
        }
        RegistryOps<JsonElement> cached = cachedOps;
        if (cached != null && cachedLookup == lookup) {
            return cached;
        }
        synchronized (ChatHoverCacheCodec.class) {
            if (cachedOps == null || cachedLookup != lookup) {
                cachedLookup = lookup;
                cachedOps = RegistryOps.create(JsonOps.INSTANCE, lookup);
            }
            return cachedOps;
        }
    }

    private static File fileFor(String key) {
        Minecraft mc = Minecraft.getInstance();
        File dir = mc.gameDirectory.toPath()
                .resolve(ConfigPaths.root())
                .resolve("betterchat")
                .resolve("cache")
                .toFile();
        return new File(dir, key + ".json");
    }
}
