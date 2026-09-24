package dev.loki.lovisual.features.gui.chat.model;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.world.item.ItemStack;
import dev.loki.lovisual.features.gui.hud.draggable.impl.BetterChat;
import dev.loki.lovisual.util.text.format.LegacyTextUtil;
import dev.loki.lovisual.features.gui.chat.render.BetterChatRenderer;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatHoverCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Per-world chat storage + async Gson persistence.
 *
 * <p>Facade keeping the public API and the active-store state; key/path resolution lives in
 * {@link ChatStoreKeys}, the async persistence engine in {@link ChatStorePersistence} and hover
 * capture in {@link ChatHoverCapture}.
 */
public enum BetterChatStoreManager {
    ;

    private static final Map<String, BetterChatStore> STORES = new HashMap<>();
    private static String currentKey = null;
    private static BetterChatStore active = null;
    private static BetterChatHoverCache activeCache = null;

    public static BetterChatStore getActiveStore(Minecraft mc) {
        String key = ChatStoreKeys.key(mc);
        if (key == null) return null;
        if (!Objects.equals(key, currentKey)) {
            switchStore(key);
        }
        return active;
    }

    public static BetterChatHoverCache getActiveCache() {
        return activeCache;
    }

    public static void addMessage(Component text) {
        Minecraft mc = Minecraft.getInstance();
        BetterChatStore store = getActiveStore(mc);
        if (store == null) return;

        text = LegacyTextUtil.convertLegacyCodes(text);

        store.add(text);
        boolean cacheChanged = ChatHoverCapture.captureHovers(text, activeCache);
        BetterChatRenderer.onNewMessage();
        BetterChat cfg = BetterChat.get();
        boolean historyOn = cfg == null || cfg.historyEnabled();
        if (historyOn) {
            ChatStorePersistence.scheduleSave(ChatStoreKeys.key(mc), store);
        }
        if (cacheChanged) {
            ChatStorePersistence.scheduleCacheSave(ChatStoreKeys.key(mc), activeCache);
        }
    }

    public static void clearActive() {
        if (active != null) {
            active.clear();
            ChatStorePersistence.scheduleSave(currentKey, active);
        }
    }

    /**
     * Persist current active store without clearing it.
     */
    public static void flushActive() {
        if (currentKey != null && active != null) {
            ChatStorePersistence.saveSync(currentKey, active);
        }
    }

    private static void switchStore(String key) {
        BetterChatRenderer.resetScroll();
        currentKey = key;
        BetterChatHoverCache cache = ChatStoreKeys.cacheFor(key);
        activeCache = cache;
        active = STORES.computeIfAbsent(key, k -> ChatStorePersistence.loadSync(k, cache));
    }

    /**
     * Flushes all pending saves and writes current state synchronously (used on shutdown).
     */
    public static void flushAll() {
        // wait for pending async saves
        ChatStorePersistence.awaitPending();
        // save active store synchronously
        if (currentKey != null && active != null) {
            ChatStorePersistence.saveSync(currentKey, active);
        }
        // save caches synchronously
        for (var entry : ChatStoreKeys.hoverCaches().entrySet()) {
            try {
                entry.getValue().save(entry.getKey());
            } catch (Exception ignored) {
            }
        }
    }

    public static String hoverItemKey(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        int compHash = stack.getComponents().hashCode();
        return "item:" + id + ":" + stack.getCount() + ":" + compHash;
    }

    public static String hoverEntityKey(HoverEvent.EntityTooltipInfo content) {
        return "ent:" + content.uuid;
    }

    public static BetterChatRenderer.Layout loadLayout() {
        return new BetterChatRenderer.Layout(16f, 120f);
    }

    public static void saveLayoutAsync(float x, float yBottom) {
    }

    public static boolean hasSavedLayout() {
        return false;
    }

    /**
     * Returns saved screen height used for the layout (framebuffer pixels), or -1 if unknown.
     */
    public static float getLayoutScreenHeight() {
        return -1f;
    }

    /**
     * Update in-memory layout cache (used every render to keep flush/save consistent).
     */
    public static void cacheLayout(float x, float yBottom, float screenH) {
    }
}
