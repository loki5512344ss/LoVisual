package dev.loki.lovisual.features.module.modules.misc.optimize;

import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.Notifier;
import dev.loki.lovisual.render.engine.optimize.entity.AnimFreeze;
import dev.loki.lovisual.render.engine.optimize.entity.ItemBounds;
import dev.loki.lovisual.render.engine.optimize.entity.ItemLod;
import dev.loki.lovisual.render.engine.optimize.hud.HudCache;
import dev.loki.lovisual.render.engine.optimize.parallel.ParallelModels;
import dev.loki.lovisual.render.engine.optimize.profile.DrawStats;
import dev.loki.lovisual.render.engine.optimize.profile.FrameBudget;
import dev.loki.lovisual.render.engine.optimize.profile.FrameProfiler;
import dev.loki.lovisual.render.engine.optimize.profile.OptimizePropertyBridge;
import dev.loki.lovisual.render.engine.optimize.OptimizeState;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.minecraft.client.Minecraft;

/**
 * Manual FPS rubilnik: one switch plus a set of knobs, each cutting a named cost
 * (the four original "rubilnik" cuts: no_glass, lite_post, lite_sky, lean_hud)
 * OR toggling a transparent optimization (entity LOD,
 * particle LOD, spawner tick-cache, etc.).
 *
 * <p>No auto-degradation by FPS — the owner's rule. Render code reads
 * {@link OptimizeState} (the original cuts), {@link OptimizeToggles} (the
 * Stage 1 transparent helpers), {@link FrameBudget} (Stage 2 frame budget)
 * — never this class; this module only pushes state into them on
 * enable/disable and once per frame.
 *
 * <p>Stage 2 of {@code mod/docs/optimize-layer-design.md}: the module also
 * pushes a {@link FrameBudget} snapshot (mode + target FPS) and resolves
 * {@code -Dlovisual.opt.*} system-property overrides via
 * {@link OptimizePropertyBridge}. Stage 3 step 1 adds {@link DrawStats} —
 * draw/pipeline/framebuffer counters fed by the {@code mixins/optimize/stats}
 * hooks, opened and closed here so they only run while F3 is up. The F3
 * overlay ({@code OptimizeDebugHudMixin}) reads both snapshots.
 */
@ModuleInfo(
        id = "optimize",
        displayName = "Optimize",
        category = ModuleCategory.MISC
)
public final class Optimize extends Module {

    // --- Original "rubilnik" cuts: visible FPS trade-offs ---
    private final BooleanValue noGlass = bool("no_glass", true);
    private final BooleanValue litePost = bool("lite_post", true);
    private final BooleanValue liteSky = bool("lite_sky", true);
    private final BooleanValue leanHud = bool("lean_hud", true);

    // --- transparent optimizations (no visual change) ---
    private final BooleanValue entityLod = bool("entity_lod", true);
    private final NumberValue<Integer> entityLodDistance =
            num("entity_lod_distance", 35, 4, 256);
    private final BooleanValue particleLod = bool("particle_lod", true);
    private final BooleanValue spawnerCull = bool("spawner_cull", true);
    private final NumberValue<Integer> spawnerDistance =
            num("spawner_distance", 16, 4, 64);
    private final BooleanValue spawnerTickCache = bool("spawner_tick_cache", true);
    private final BooleanValue blockEntityCache = bool("block_entity_cache", true);
    private final BooleanValue preparedTextCache = bool("prepared_text_cache", true);
    private final BooleanValue microOpts = bool("micro_opts", true);

    // --- Stage 3: GL-level helpers ---
    private final BooleanValue fboShare = bool("fbo_share", true);
    private final BooleanValue entityAnimFreeze = bool("entity_anim_freeze", false);
    private final NumberValue<Integer> entityAnimDistance =
            num("entity_anim_distance", 16, 4, 128);

    // --- Stage 3: items ---
    private final BooleanValue itemLod = bool("item_lod", true);
    private final NumberValue<Integer> itemLodDistance = num("item_lod_distance", 8, 2, 64);
    private final BooleanValue itemBounds = bool("item_bounds", true);

    // --- Stage 3: HUD cache (off by default — interacts with the deferred-HUD markers) ---
    private final BooleanValue hudCacheToggle = bool("hud_cache", false);

    // --- Stage 3: parallel model build (own holder: cores/Iris gating inside) ---
    private final BooleanValue parallelModels = bool("parallel_models", true);

    // --- Stage 2: frame budget + profiling ---
    private final EnumValue<FrameBudget.Mode> budgetMode =
            enumMode("budget_mode", FrameBudget.Mode.OFF, FrameBudget.Mode.values());
    private final NumberValue<Integer> targetFps =
            num("target_fps", FrameBudget.DEFAULT_TARGET_FPS, 15, 360);
    private final BooleanValue showProfiler =
            bool("show_profiler", false);

    private boolean notified;

    @Override
    public void onEnable() {
        pushState();
        if (!notified) {
            notified = true;
            Notifier.info(
                    "Optimize: "
                            + summaryOf(noGlass.get(), litePost.get(), liteSky.get(), leanHud.get()));
        }
    }

    @Override
    public void onDisable() {
        OptimizeState.reset();
        OptimizeToggles.reset();
        FrameBudget.reset();
        AnimFreeze.reset();
        ItemLod.reset();
        ItemBounds.reset();
        HudCache.reset();
        ParallelModels.reset();
        // stop counting and drop the F3 numbers, onFrame will not run again
        DrawStats.endFrame(false);
    }

    /** Picks up knob changes made while the module stays on. */
    @Override
    public void onFrame(float d) {
        pushState();
        // close the window of the frame that just drew; the next publish lands once a second
        DrawStats.endFrame(debugOverlayVisible());
    }

    /** F3 counts are only worth collecting while they are on screen. */
    private static boolean debugOverlayVisible() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null && mc.getDebugOverlay() != null && mc.getDebugOverlay().showDebugScreen();
    }

    private void pushState() {
        boolean on = isEnabled();
        // Original four "rubilnik" cuts.
        OptimizeState.apply(
                on && noGlass.get(),
                on && litePost.get(),
                on && liteSky.get(),
                on && leanHud.get());

        // Stage 1 toggles — wire through the -Dlovisual.opt.* overrides.
        OptimizePropertyBridge.Snapshot props = OptimizePropertyBridge.resolve();
        OptimizeToggles.apply(
                resolveBool(props, props.entityLod, on && entityLod.get()),
                resolveInt(props, props.entityLodDistance, entityLodDistance.get()),
                resolveBool(props, props.particleLod, on && particleLod.get()),
                resolveBool(props, props.particleLod, on && particleLod.get()),
                resolveBool(props, props.particleLod, on && particleLod.get()),
                resolveBool(props, props.spawnerCull, on && spawnerCull.get()),
                resolveInt(props, props.spawnerDistance, spawnerDistance.get()),
                resolveBool(props, props.spawnerTickCache, on && spawnerTickCache.get()),
                resolveBool(props, props.blockEntityCache, on && blockEntityCache.get()),
                resolveBool(props, props.preparedTextCache, on && preparedTextCache.get()),
                resolveBool(props, props.microOpts, on && microOpts.get()),
                resolveBool(props, props.fboShare, on && fboShare.get()));

        // Stage 2: frame budget. -Dlovisual.opt.budget_mode / target_fps override the module.
        FrameBudget.Mode mode = on ? budgetMode.get() : FrameBudget.Mode.OFF;
        FrameBudget.apply(props.overrides(mode, mode), props.resolveTargetFps(targetFps.get()));

        // Stage 3: far-entity animation freeze — own holder, pushed once per frame.
        AnimFreeze.configure(on && entityAnimFreeze.get(), entityAnimDistance.get());

        // Stage 3: far-item LOD + shared single-layer item bounds.
        ItemLod.configure(on && itemLod.get(), itemLodDistance.get());
        ItemBounds.configure(on && itemBounds.get());

        // Stage 3: HUD-at-refresh-rate cache; own holder (foreign-HUD/Vulkan gating inside).
        HudCache.configure(on && hudCacheToggle.get());

        // Stage 3: parallel model build; mutually exclusive with anim freeze (checked inside).
        ParallelModels.configure(on && parallelModels.get());
    }

    private static boolean resolveBool(OptimizePropertyBridge.Snapshot props,
                                       Boolean override, boolean moduleValue) {
        return props.overrides(override, moduleValue);
    }

    private static int resolveInt(OptimizePropertyBridge.Snapshot props,
                                  int override, int moduleValue) {
        return props.overrides(override, moduleValue);
    }

    /** Compact list of what is being cut, for the first-enable notice. */
    static String summaryOf(boolean glass, boolean post, boolean sky, boolean hud) {
        StringBuilder out = new StringBuilder();
        if (glass) {
            append(out, "no glass");
        }
        if (post) {
            append(out, "light post");
        }
        if (sky) {
            append(out, "simple sky");
        }
        if (hud) {
            append(out, "lean HUD");
        }
        if (out.length() == 0) {
            return "no knobs active";
        }
        return out.toString();
    }

    private static void append(StringBuilder out, String knob) {
        if (out.length() > 0) {
            out.append(", ");
        }
        out.append(knob);
    }
}
