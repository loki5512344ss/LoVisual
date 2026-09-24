package dev.loki.lovisual.addon.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Owns the id → addon-id reverse indexes for contributed modules, draggable-HUD and static-HUD
 * elements, and turns them into the namespaced config-profile owner keys the profile system uses
 * ({@code addon:<addon>:module:<id>} when an addon owns the entry, plain otherwise). Extracted
 * from {@link AddonManager} to keep it under the file-size cap; shares {@code addon.core}'s
 * package so {@link AddonManager#normalizeId} stays reachable.
 */
final class AddonProfiles {

    private static final Map<String, String> MODULE_OWNERS = new LinkedHashMap<>();
    private static final Map<String, String> DRAGGABLE_HUD_OWNERS = new LinkedHashMap<>();
    private static final Map<String, String> STATIC_HUD_OWNERS = new LinkedHashMap<>();

    private AddonProfiles() { }

    /** Record that {@code addonId} owns the given module / draggable-HUD / static-HUD element. */
    static void noteModuleOwner(String addonId, String moduleId) {
        if (moduleId != null && !moduleId.isBlank()) {
            MODULE_OWNERS.put(AddonManager.normalizeId(moduleId), AddonManager.normalizeId(addonId));
        }
    }

    static void noteDraggableHudOwner(String addonId, String elementId) {
        if (elementId != null && !elementId.isBlank()) {
            DRAGGABLE_HUD_OWNERS.put(AddonManager.normalizeId(elementId), AddonManager.normalizeId(addonId));
        }
    }

    static void noteStaticHudOwner(String addonId, String elementId) {
        if (elementId != null && !elementId.isBlank()) {
            STATIC_HUD_OWNERS.put(AddonManager.normalizeId(elementId), AddonManager.normalizeId(addonId));
        }
    }

    /** Namespaced config-profile owner key for a module. */
    static String moduleProfileOwnerId(String moduleId) {
        String addonId = MODULE_OWNERS.get(AddonManager.normalizeId(moduleId));
        return addonId == null ? "module:" + moduleId : "addon:" + addonId + ":module:" + moduleId;
    }

    /** Namespaced config-profile owner key for a draggable HUD element. */
    static String draggableHudProfileOwnerId(String elementId) {
        String addonId = DRAGGABLE_HUD_OWNERS.get(AddonManager.normalizeId(elementId));
        return addonId == null ? "hud.draggable:" + elementId : "addon:" + addonId + ":hud.draggable:" + elementId;
    }

    /** Namespaced config-profile owner key for a static HUD element. */
    static String staticHudProfileOwnerId(String elementId) {
        String addonId = STATIC_HUD_OWNERS.get(AddonManager.normalizeId(elementId));
        return addonId == null ? "hud.static:" + elementId : "addon:" + addonId + ":hud.static:" + elementId;
    }

    /** Extract the owning addon id from an {@code addon:<id>:...} owner key, or {@code null}. */
    static String addonIdFromProfileOwner(String ownerId) {
        if (ownerId == null || !ownerId.startsWith("addon:")) return null;
        int start = "addon:".length();
        int end = ownerId.indexOf(':', start);
        if (end <= start) return null;
        return AddonManager.normalizeId(ownerId.substring(start, end));
    }
}
