package dev.loki.lovisual.features.module.core;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.Lifecycle;
import dev.loki.lovisual.features.module.lifecycle.ModuleActivationSource;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.phase.ModuleStateListener;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.logging.DebugLog;
import dev.loki.lovisual.util.screen.ClientScreen;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.SubmitNodeCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Singleton entry point for module registration, lookup and per-frame dispatch.
 *
 * <p>Delegates the heavy work to {@link ModuleRegistrar} (validate + insert +
 * rebuild array snapshots) and {@link ModuleDispatcher} (tick / HUD / world /
 * keybind loops), keeping the public static API consumed by the ClickGUI,
 * commands, HUD, add-on bridge and {@code LoVisualClientApi} unchanged.</p>
 */
public enum ModuleManager {
    ;

    // Indexes + snapshots (package-private so Registrar / Dispatcher can share them) --------
    static final List<Module> modules = new ArrayList<>();
    private static final List<Module> modulesView = Collections.unmodifiableList(modules);
    static final Map<String, Module> byId = new LinkedHashMap<>();
    static final Map<Class<? extends Module>, Module> byType = new IdentityHashMap<>();
    static final EnumMap<HudPhase, List<Module>> HUD_PHASE_MODULES = new EnumMap<>(HudPhase.class);
    static final EnumMap<HudPhase, Module[]> HUD_PHASE_SNAPSHOTS = new EnumMap<>(HudPhase.class);
    static final EnumMap<WorldPhase, List<Module>> WORLD_PHASE_MODULES = new EnumMap<>(WorldPhase.class);
    static final EnumMap<WorldPhase, Module[]> WORLD_PHASE_SNAPSHOTS = new EnumMap<>(WorldPhase.class);
    private static final List<ModuleStateListener> listeners = new ArrayList<>();
    static Module[] modulesSnapshot = new Module[0];
    private static ModuleStateListener[] listenerSnapshot = new ModuleStateListener[0];
    @Setter private static boolean suppressToggleSound = false;
    @Setter private static boolean suppressToggleNotifications = false;

    static {
        for (HudPhase phase : HudPhase.values()) { HUD_PHASE_MODULES.put(phase, new ArrayList<>()); HUD_PHASE_SNAPSHOTS.put(phase, new Module[0]); }
        for (WorldPhase phase : WorldPhase.values()) { WORLD_PHASE_MODULES.put(phase, new ArrayList<>()); WORLD_PHASE_SNAPSHOTS.put(phase, new Module[0]); }
    }

    /** Validate + insert {@code module}; delegates to {@link ModuleRegistrar}. */
    public static void register(Module module) { ModuleRegistrar.register(module); }

    // Lookup / toggle ----------------------------------------------------------------------
    public static Module get(String name) {
        if (name == null || name.isBlank()) return null;
        return byId.get(ModuleRegistrar.normalizeName(name));
    }

    public static <T extends Module> T get(Class<T> type) {
        if (type == null) return null;
        Module module = byType.get(type);
        return module == null ? null : type.cast(module);
    }

    /** Same as {@link #get(Class)} but throws when the module is not registered. */
    public static <T extends Module> T require(Class<T> type) {
        T module = get(type);
        if (module == null) throw new NoSuchElementException("Module is not registered: " + (type == null ? "<null>" : type.getName()));
        return module;
    }

    public static ModuleCategory getCategory(String name) {
        Module m = get(name);
        return m != null ? m.getCategory() : null;
    }

    public static void toggle(String name) { Module m = get(name); if (m != null) m.toggle(); }
    public static void toggle(String name, ModuleActivationSource source) { Module m = get(name); if (m != null) m.setEnabled(!m.isEnabled(), source); }

    // Per-frame dispatch (delegates to ModuleDispatcher) -----------------------------------
    public static void tickAll() { ModuleDispatcher.tickAll(); }
    public static void frameAll(float frameDeltaTicks) { ModuleDispatcher.frameAll(frameDeltaTicks); }
    public static void renderHud(HudPhase phase, GuiGraphicsExtractor ctx, float tickDelta) { ModuleDispatcher.renderHud(phase, ctx, tickDelta); }
    public static void renderHudEngine(HudPhase phase, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) { ModuleDispatcher.renderHudEngine(phase, renderer, textRenderer, ctx, tickDelta); }
    public static void renderHudEngineForeground(HudPhase phase, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) { ModuleDispatcher.renderHudEngineForeground(phase, renderer, textRenderer, ctx, tickDelta); }
    public static void renderWorld(WorldPhase phase, PoseStack matrices, SubmitNodeCollector consumers, float tickDelta) { ModuleDispatcher.renderWorld(phase, matrices, consumers, tickDelta); }
    public static void renderWorldEngine(WorldPhase phase, Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) { ModuleDispatcher.renderWorldEngine(phase, renderer, depthRenderer, tickDelta); }
    public static boolean hasHudEngineWork(HudPhase phase) { return ModuleDispatcher.hasEnabledHudPhaseModule(phase); }
    public static boolean hasHudEngineForegroundWork(HudPhase phase) { return ModuleDispatcher.hasEnabledHudPhaseModule(phase); }
    public static void handleModuleKeybinds() { ModuleDispatcher.handleModuleKeybinds(); }

    // Enable / disable ---------------------------------------------------------------------
    public static boolean isEnabled(String name) { Module m = get(name); return m != null && m.isEnabled(); }

    public static void setEnabled(String name, boolean enabled) { setEnabled(name, enabled, ModuleActivationSource.MANUAL); }

    public static void setEnabled(String name, boolean enabled, ModuleActivationSource source) {
        Module m = get(name);
        if (m == null) return;
        if (enabled && !Lifecycle.isActive()) return;
        if (enabled && name.equals("clickgui") && ClientScreen.current(Minecraft.getInstance()) != null) return;
        m.setEnabled(enabled, source);
    }

    /** Enable or disable a module without firing persistence or keybind hooks. */
    public static boolean setRuntimeEnabledTransient(String name, boolean enabled) {
        Module m = get(name);
        if (m == null) return false;
        m.setRuntimeEnabledTransient(enabled);
        return true;
    }

    /** Disable every currently-enabled module; toggle notifications are suppressed for the whole pass. */
    public static void disableAll() {
        boolean prev = suppressToggleNotifications; suppressToggleNotifications = true;
        try { for (Module m : modulesSnapshot) if (m != null && m.isEnabled()) m.setEnabled(false, ModuleActivationSource.INTERNAL); }
        finally { suppressToggleNotifications = prev; }
    }

    /** Bulk-apply a config profile: enable exactly the modules named, disable everything else, without firing per-toggle notifications. */
    public static void setEnabledModules(List<String> names) {
        if (!Lifecycle.isActive()) return;
        boolean prev = suppressToggleNotifications;
        suppressToggleNotifications = true;
        try {
            ObjectOpenHashSet<String> enabledNames = new ObjectOpenHashSet<>();
            if (names != null) for (String name : names) if (name != null && !name.isBlank()) enabledNames.add(ModuleRegistrar.normalizeName(name));
            for (Module m : modulesSnapshot) {
                boolean shouldEnable = enabledNames.contains(ModuleRegistrar.normalizeName(m.name()));
                if (m.isEnabled() != shouldEnable) m.setEnabled(shouldEnable, ModuleActivationSource.INTERNAL);
            }
        } finally { suppressToggleNotifications = prev; }
    }

    // Bulk queries -------------------------------------------------------------------------
    /** Unmodifiable insertion-order view of every registered module. */
    public static List<Module> getModules() { return modulesView; }

    /** Names of every registered module; consumed by the ClickGUI module list. */
    public static List<String> getAllModules() { return listMatched(m -> true, Module::name); }
    public static List<String> getEnabledModulesNames() { return listMatched(Module::isEnabled, Module::name); }
    public static List<Module> getKeybindEnabledModules() { return listMatched(Module::isEnabledFromKeybind, m -> m); }
    public static int registeredCount() { return modulesSnapshot.length; }
    private static <T> List<T> listMatched(java.util.function.Predicate<Module> filter, java.util.function.Function<Module, T> mapper) {
        List<T> out = new ArrayList<>();
        for (Module m : modulesSnapshot) if (filter.test(m)) out.add(mapper.apply(m));
        return out;
    }

    // State listeners ----------------------------------------------------------------------
    public static void addListener(ModuleStateListener listener) {
        if (listener == null) return;
        listeners.add(listener);
        listenerSnapshot = listeners.toArray(ModuleStateListener[]::new);
    }

    /** Fan out a toggle event to every listener; suppressed inside a batched config apply. */
    public static void notifyListeners(String name, boolean enabled) {
        if (suppressToggleNotifications) return;
        for (ModuleStateListener l : listenerSnapshot) l.onModuleStateChanged(name, enabled);
    }

    // Toggle-sound / notification suppression ---------------------------------------------
    public static boolean isToggleSoundSuppressed() { return suppressToggleSound; }
    public static boolean isToggleNotificationSuppressed() { return suppressToggleNotifications; }

    /** Runs {@code action} with both toggle sounds and module-state notifications suppressed. */
    public static void runSilentConfigApply(Runnable action) {
        if (action == null) return;
        boolean ps = suppressToggleSound, pn = suppressToggleNotifications;
        suppressToggleSound = true; suppressToggleNotifications = true;
        try { action.run(); } finally { suppressToggleSound = ps; suppressToggleNotifications = pn; }
    }

    // Config orchestration ----------------------------------------------------------------
    /** Load + apply config for every registered module; logs per-module failures without aborting. */
    public static void loadAllModuleConfigs() {
        DebugLog.config("loadAllModuleConfigs: %d modules", modulesSnapshot.length);
        for (Module m : modulesSnapshot) try { DebugLog.config("Loading module config -> %s", m.name()); m.loadAndApplyConfig(); }
        catch (Throwable ignored) { DebugLog.error("Failed to load config for module %s", ignored, m.name()); }
    }

    /** Save config for every registered module; logs per-module failures without aborting. */
    public static void saveAllModuleConfigs() { for (Module m : modulesSnapshot) try { m.saveConfig(); }
    catch (Throwable ignored) { DebugLog.error("Failed to save config for module %s", ignored, m.name()); } }
}
