package dev.loki.lovisual.features.gui.chat.text.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Per-key cache for ShowItem/ShowEntity hover payloads to persist across sessions.
 * Stores compact deduped entries; keyed by current chat store key (dimension/server).
 *
 * <p>Disk (de)serialization, registry-ops codec and display-name indexing live in
 * {@link ChatHoverCacheCodec}; this class keeps the public put/get/find API and per-instance maps.
 */
public final class BetterChatHoverCache {

    final Map<String, ItemEntry> itemCache = new HashMap<>();
    final Map<String, EntityEntry> entityCache = new HashMap<>();
    private final Map<String, ItemStack> decodedItemCache = new HashMap<>();
    final Map<String, ItemLookup> itemLookupByName = new HashMap<>();
    final Map<String, EntityLookup> entityLookupByName = new HashMap<>();
    final Set<String> loadedItemKeys = new HashSet<>();
    final Set<String> loadedEntityKeys = new HashSet<>();
    boolean loadedFromDisk = false;
    boolean itemLookupIndexBuilt = false;
    boolean entityLookupIndexBuilt = false;

    private BetterChatHoverCache() {
    }

    public static BetterChatHoverCache load(String key) {
        BetterChatHoverCache cache = new BetterChatHoverCache();
        ChatHoverCacheCodec.populate(cache, key);
        return cache;
    }

    public void save(String key) {
        ChatHoverCacheCodec.write(this, key);
    }

    public boolean putItem(String keyId, ItemStack stack) {
        BetterChat cfg = BetterChat.get();
        if (cfg != null && !cfg.cacheItems()) return false;
        if (keyId == null || stack == null || stack.isEmpty()) return false;
        try {
            String itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            String compJson = ChatHoverCacheCodec.encodeComponents(stack);
            if (compJson == null) return false;
            ItemEntry entry = new ItemEntry(itemId, compJson);
            ItemEntry prev = itemCache.put(keyId, entry);
            decodedItemCache.put(keyId, stack.copy());
            itemLookupIndexBuilt = false;
            clamp(cacheLimit());
            return !entry.equals(prev);
        } catch (Exception e) {
            DebugLog.error("BetterChatHoverCache putItem failed", e);
            return false;
        }
    }

    public boolean putEntity(String keyId, HoverEvent.EntityTooltipInfo content) {
        BetterChat cfg = BetterChat.get();
        if (cfg != null && !cfg.cacheEntities()) return false;
        if (keyId == null || content == null) return false;

        String name = content.name.map(Component::getString).orElse("").trim();
        if (name.isEmpty()) return false;

        if (content.type != net.minecraft.world.entity.EntityTypes.PLAYER) {
            String def = content.type.getDescription().getString().trim();
            if (!def.isEmpty() && def.equalsIgnoreCase(name)) {
                return false;
            }
        }

        String type = BuiltInRegistries.ENTITY_TYPE.getKey(content.type).toString();
        String uuid = content.uuid.toString();
        EntityEntry entry = new EntityEntry(name, type, uuid);
        EntityEntry prev = entityCache.put(keyId, entry);
        entityLookupIndexBuilt = false;
        clamp(cacheLimit());
        return !entry.equals(prev);
    }

    public ItemStack getItem(String keyId) {
        BetterChat cfg = BetterChat.get();
        if (cfg != null && !cfg.cacheItems()) return ItemStack.EMPTY;
        ItemEntry e = itemCache.get(keyId);
        if (e == null) return ItemStack.EMPTY;
        ItemStack cached = decodedItemCache.get(keyId);
        if (cached != null && !cached.isEmpty()) {
            return cached.copy();
        }
        ItemStack decoded = ChatHoverCacheCodec.decodeItem(e.componentsJson());
        if (decoded != null) {
            decodedItemCache.put(keyId, decoded.copy());
            return decoded;
        }
        return ItemStack.EMPTY;
    }

    public HoverEvent.EntityTooltipInfo getEntity(String keyId) {
        BetterChat cfg = BetterChat.get();
        if (cfg != null && !cfg.cacheEntities()) return null;
        EntityEntry e = entityCache.get(keyId);
        if (e == null) return null;
        try {
            return new HoverEvent.EntityTooltipInfo(
                    BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse(e.typeId())),
                    UUID.fromString(e.uuid()),
                    e.name().isEmpty() ? null : Component.literal(e.name())
            );
        } catch (Exception ex) {
            DebugLog.error("BetterChatHoverCache getEntity build failed", ex);
            return null;
        }
    }

    private void clamp(int limit) {
        int max = Math.max(64, limit);
        while (itemCache.size() > max) {
            var it = itemCache.keySet().iterator();
            if (it.hasNext()) {
                String key = it.next();
                it.remove();
                decodedItemCache.remove(key);
                itemLookupIndexBuilt = false;
            }
        }
        while (entityCache.size() > max) {
            var it = entityCache.keySet().iterator();
            if (it.hasNext()) {
                it.next();
                it.remove();
                entityLookupIndexBuilt = false;
            }
        }
    }

    private int cacheLimit() {
        BetterChat cfg = BetterChat.get();
        return cfg != null ? cfg.cacheLimit() : 512;
    }

    public ItemStack recoverItemById(String itemId) {
        if (itemId == null) return ItemStack.EMPTY;
        BetterChat cfg = BetterChat.get();
        if (cfg != null && !cfg.cacheItems()) return ItemStack.EMPTY;
        for (var entry : itemCache.entrySet()) {
            if (itemId.equals(entry.getValue().getId())) {
                return getItem(entry.getKey());
            }
        }
        return ItemStack.EMPTY;
    }

    public ItemStack getItemByKeyOrId(String keyId, String itemId) {
        ItemStack byKey = getItem(keyId);
        if (!byKey.isEmpty()) return byKey;
        return recoverItemById(itemId);
    }

    public boolean hasItemId(String itemId) {
        if (itemId == null) return false;
        return itemCache.values().stream().anyMatch(e -> itemId.equals(e.getId()));
    }

    public boolean wasLoadedFromDisk() {
        return loadedFromDisk;
    }

    public boolean wasItemKeyLoaded(String keyId) {
        return loadedItemKeys.contains(keyId);
    }

    public boolean wasEntityKeyLoaded(String keyId) {
        return loadedEntityKeys.contains(keyId);
    }

    public ItemLookup findItemByDisplayNameWithMeta(String displayName) {
        if (displayName == null || displayName.isEmpty()) return new ItemLookup(ItemStack.EMPTY, false);
        BetterChat cfg = BetterChat.get();
        if (cfg != null && !cfg.cacheItems()) return new ItemLookup(ItemStack.EMPTY, false);
        String needle = ChatHoverCacheCodec.normalizeLookup(displayName);
        if (needle.isEmpty()) return new ItemLookup(ItemStack.EMPTY, false);
        ChatHoverCacheCodec.ensureItemLookupIndex(this);
        ItemLookup lookup = itemLookupByName.get(needle);
        if (lookup == null || lookup.stack().isEmpty()) {
            return new ItemLookup(ItemStack.EMPTY, false);
        }
        return new ItemLookup(lookup.stack().copy(), lookup.loadedFromFile());
    }

    public ItemStack findItemByDisplayName(String displayName) {
        return findItemByDisplayNameWithMeta(displayName).stack();
    }

    public EntityLookup findEntityByNameWithMeta(String name) {
        if (name == null || name.isEmpty()) return new EntityLookup(null, false);
        BetterChat cfg = BetterChat.get();
        if (cfg != null && !cfg.cacheEntities()) return new EntityLookup(null, false);
        String needle = ChatHoverCacheCodec.normalizeLookup(name);
        if (needle.isEmpty()) return new EntityLookup(null, false);
        ChatHoverCacheCodec.ensureEntityLookupIndex(this);
        EntityLookup lookup = entityLookupByName.get(needle);
        return lookup != null ? lookup : new EntityLookup(null, false);
    }

    public HoverEvent.EntityTooltipInfo findEntityByName(String name) {
        return findEntityByNameWithMeta(name).content();
    }

    public record ItemEntry(String id, String componentsJson) {
        public String getId() {
            return id;
        }
    }

    public record EntityEntry(String name, String typeId, String uuid) {
    }

    public record ItemLookup(ItemStack stack, boolean loadedFromFile) {
    }

    public record EntityLookup(HoverEvent.EntityTooltipInfo content, boolean loadedFromFile) {
    }
}
