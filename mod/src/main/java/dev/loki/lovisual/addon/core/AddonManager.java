package dev.loki.lovisual.addon.core;

import dev.loki.lovisual.addon.pipeline.AddonSnapshot;
import dev.loki.lovisual.addon.pipeline.AddonStateStore;
import dev.loki.lovisual.addon.pipeline.AddonStatus;
import dev.loki.lovisual.features.module.lifecycle.ModuleAutoLoader;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Singleton entry point for LoVisual add-on discovery and lifecycle orchestration. Keeps the
 * addon registry ({@code ADDONS}), the persisted enabled-overrides and the client-ready flags, and
 * drives discovery, init and the public lookup/enable API consumed by the ClickGUI and commands.
 *
 * <p>Delegates the heavy work within the same package: {@link AddonLifecycle} (per-registration
 * callbacks), {@link AddonRuntime} (enable/disable + contribution suspend/resume),
 * {@link AddonDescriptors} (metadata parsing + validation) and {@link AddonProfiles} (config-profile
 * owner keys), keeping the public static API stable while each file stays under the size caps.</p>
 */
public enum AddonManager {
    ;

    public static final String ADDON_METADATA_KEY = "lovisual:addon";

    static final Map<String, AddonRegistration> ADDONS = new LinkedHashMap<>();
    static final Map<String, Boolean> ENABLED_OVERRIDES = new LinkedHashMap<>();
    private static boolean discovered;
    private static boolean moduleLoadingPrepared;
    private static boolean initialized;
    static boolean clientReady;

    /**
     * Discovers enabled addon entrypoints and lets them configure built-in module
     * exclusions before {@code ModuleAutoLoader} starts instantiating modules.
     */
    public static void prepareModuleLoading() {
        ensureDiscovered();
        if (moduleLoadingPrepared) return;
        moduleLoadingPrepared = true;
        AddonLifecycle.configureEnabled();
    }

    public static void init() {
        if (initialized) return;
        prepareModuleLoading();
        if (!ModuleAutoLoader.isDiscoveryFinished()) {
            return;
        }
        AddonLifecycle.reconcileExclusions();
        initialized = true;
        AddonLifecycle.initializeEnabled();
    }

    /**
     * Completes addon startup after module configs have been loaded and applied.
     */
    public static void notifyClientReady() {
        init();
        if (!initialized) return;
        clientReady = true;
        for (AddonRegistration registration : ADDONS.values()) {
            AddonLifecycle.notifyClientReady(registration);
        }
    }

    private static void ensureDiscovered() {
        if (discovered) return;
        discovered = true;
        ENABLED_OVERRIDES.clear();
        ENABLED_OVERRIDES.putAll(AddonStateStore.loadEnabledOverrides());
        AddonLifecycle.discover();
    }

    public static List<AddonSnapshot> snapshots() {
        init();
        return ADDONS.values().stream()
                .map(AddonRegistration::snapshot)
                .sorted(Comparator.comparing(AddonSnapshot::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public static AddonSnapshot snapshot(String id) {
        init();
        AddonRegistration registration = ADDONS.get(normalizeId(id));
        return registration == null ? null : registration.snapshot();
    }

    public static boolean isActive(String id) {
        init();
        AddonRegistration registration = ADDONS.get(normalizeId(id));
        return registration != null && registration.enabled && registration.status == AddonStatus.ACTIVE;
    }

    public static List<AddonIssue> scan() {
        init();
        List<AddonIssue> out = new ArrayList<>();
        for (AddonRegistration registration : ADDONS.values()) {
            AddonDescriptors.validate(registration);
            out.addAll(registration.issues);
        }
        if (out.isEmpty()) {
            out.add(new AddonIssue("lovisual", AddonIssue.Severity.INFO, "No addon errors found", ""));
        }
        return List.copyOf(out);
    }

    public static String moduleProfileOwnerId(String moduleId) {
        return AddonProfiles.moduleProfileOwnerId(moduleId);
    }

    public static String draggableHudProfileOwnerId(String elementId) {
        return AddonProfiles.draggableHudProfileOwnerId(elementId);
    }

    public static String staticHudProfileOwnerId(String elementId) {
        return AddonProfiles.staticHudProfileOwnerId(elementId);
    }

    public static String profileDisplayName(String ownerId, String displayName) {
        String addonId = AddonProfiles.addonIdFromProfileOwner(ownerId);
        if (addonId == null) return displayName;
        AddonRegistration registration = ADDONS.get(addonId);
        String addonName = registration == null ? addonId : registration.descriptor.name();
        return addonName + " / " + displayName;
    }

    public static String addonIdFromProfileOwner(String ownerId) {
        return AddonProfiles.addonIdFromProfileOwner(ownerId);
    }

    public static boolean setEnabled(String id, boolean enabled) {
        init();
        String normalizedId = normalizeId(id);
        AddonRegistration registration = ADDONS.get(normalizedId);
        if (registration == null) return false;
        if (registration.enabled == enabled) return true;

        registration.enabled = enabled;
        registration.restartRequired = false;
        ENABLED_OVERRIDES.put(normalizedId, enabled);
        AddonStateStore.saveEnabledOverrides(ENABLED_OVERRIDES);

        if (enabled) {
            AddonRuntime.enable(registration, clientReady);
        } else {
            AddonRuntime.disable(registration);
        }
        return true;
    }

    public static int activeCount() {
        int count = 0;
        for (AddonRegistration registration : ADDONS.values()) {
            if (registration.status == AddonStatus.ACTIVE) count++;
        }
        return count;
    }

    public static void noteModuleOwner(String addonId, String moduleId) {
        AddonProfiles.noteModuleOwner(addonId, moduleId);
    }

    public static void noteDraggableHudOwner(String addonId, String elementId) {
        AddonProfiles.noteDraggableHudOwner(addonId, elementId);
    }

    public static void noteStaticHudOwner(String addonId, String elementId) {
        AddonProfiles.noteStaticHudOwner(addonId, elementId);
    }

    /** Locale-independent lowercase key used for every addon / module / HUD id lookup. */
    static String normalizeId(String id) {
        return id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
    }
}
