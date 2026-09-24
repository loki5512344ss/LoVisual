package dev.loki.lovisual.features.module.core;

import dev.loki.lovisual.events.Events;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.util.logging.DebugLog;

import java.util.List;
import java.util.Locale;

/**
 * Registration pipeline backing {@link ModuleManager}. Validates a module's
 * identity (annotation, id, class, instance), inserts it into the three
 * lookup indexes ({@code byId}, {@code byType}, insertion-order list),
 * buckets it into the HUD / world phase caches and rebuilds the array views
 * consumed by the per-frame render loops. Sibling folder sharing the parent's
 * package so {@link ModuleManager}'s package-private indexes stay reachable.
 */
final class ModuleRegistrar {

    private ModuleRegistrar() { }

    /** Validate + insert {@code module} into every index, then rebuild snapshots; logs and skips on any conflict. */
    static void register(Module module) {
        if (module == null) { DebugLog.error("Skipping null module registration"); return; }
        String id = module.name();
        if (id == null || id.isBlank()) { DebugLog.error("Skipping module with blank id: %s", null, module.getClass().getName()); return; }
        Class<? extends Module> type = module.getClass();
        if (!type.isAnnotationPresent(ModuleInfo.class)) { DebugLog.error("Skipping module without metadata annotation: %s", null, type.getName()); return; }
        String normalizedId = normalizeName(id);
        Module existingById = ModuleManager.byId.get(normalizedId);
        if (existingById != null) { DebugLog.error("Skipping duplicate module id '%s': existing=%s duplicate=%s", null, normalizedId, existingById.getClass().getName(), type.getName()); return; }
        if (ModuleManager.byType.get(type) != null) { DebugLog.error("Skipping duplicate module class: %s", null, type.getName()); return; }
        if (ModuleManager.modules.contains(module)) { DebugLog.error("Skipping duplicate module instance: %s", null, type.getName()); return; }
        ModuleManager.modules.add(module);
        ModuleManager.byId.put(normalizedId, module);
        ModuleManager.byType.put(type, module);
        cacheHudPhase(module);
        cacheWorldPhase(module);
        rebuildSnapshots();
        Events.BUS.register(module);
    }

    /** Locale-independent lowercase key used for id lookups; also consumed by the facade. */
    static String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private static void cacheHudPhase(Module module) {
        HudPhase phase = module.getHudPhase();
        if (phase == null || phase == HudPhase.NONE) return;
        ModuleManager.HUD_PHASE_MODULES.get(phase).add(module);
    }

    private static void cacheWorldPhase(Module module) {
        WorldPhase phase = module.getWorldPhase();
        if (phase == null || phase == WorldPhase.NONE) return;
        ModuleManager.WORLD_PHASE_MODULES.get(phase).add(module);
    }

    /** Flatten every lookup list into the array views the render loops iterate on the client thread. */
    static void rebuildSnapshots() {
        ModuleManager.modulesSnapshot = ModuleManager.modules.toArray(Module[]::new);
        for (HudPhase phase : HudPhase.values()) {
            List<Module> list = ModuleManager.HUD_PHASE_MODULES.get(phase);
            ModuleManager.HUD_PHASE_SNAPSHOTS.put(phase, list == null || list.isEmpty() ? new Module[0] : list.toArray(Module[]::new));
        }
        for (WorldPhase phase : WorldPhase.values()) {
            List<Module> list = ModuleManager.WORLD_PHASE_MODULES.get(phase);
            ModuleManager.WORLD_PHASE_SNAPSHOTS.put(phase, list == null || list.isEmpty() ? new Module[0] : list.toArray(Module[]::new));
        }
    }
}
