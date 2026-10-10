package dev.loki.lovisual.features.module.modules.misc.optimize;

import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.Notifier;
import dev.loki.lovisual.render.engine.optimize.profile.FrameBudget;
import dev.loki.lovisual.render.engine.optimize.profile.OptimizePropertyBridge;
import dev.loki.lovisual.render.engine.optimize.OptimizeState;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;

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
 * <p>Stage 2 of {@code mod/docs/optimize-layer-design.md}: the module now also
 * pushes a {@link FrameBudget} snapshot (mode + target FPS) and resolves
 * {@code -Dlovisual.opt.*} system-property overrides via
 * {@link OptimizePropertyBridge}. The frame profiler ({@code FrameProfiler})
 * is fed by the {@code MinecraftFrameMixin} (Stage 3 — not yet present); the
 * F3 overlay ({@code OptimizeDebugHudMixin}) reads the profiler's snapshot
 * whenever the user opens F3.
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
    }

    /** Picks up knob changes made while the module stays on. */
    @Override
    public void onFrame(float d) {
        pushState();
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
                resolveBool(props, props.microOpts, on && microOpts.get()));

        // Stage 2: frame budget. -Dlovisual.opt.budget_mode / target_fps override the module.
        FrameBudget.Mode mode = on ? budgetMode.get() : FrameBudget.Mode.OFF;
        FrameBudget.apply(props.overrides(mode, mode), props.resolveTargetFps(targetFps.get()));
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
