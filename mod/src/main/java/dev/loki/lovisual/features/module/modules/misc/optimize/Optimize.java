package dev.loki.lovisual.features.module.modules.misc.optimize;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.Notifier;
import dev.loki.lovisual.render.engine.optimize.OptimizeState;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;

/**
 * Manual FPS rubilnik: one switch plus a set of knobs, each cutting a named cost
 * (the four original "rubilnik" cuts: no_glass, lite_post, lite_sky, lean_hud)
 * OR toggling a transparent optimization (entity LOD,
 * particle LOD, spawner tick-cache, etc.).
 *
 * <p>No auto-degradation by FPS — the owner's rule. Render code reads
 * {@link OptimizeState} (the original cuts) and {@link OptimizeToggles} (the
 * new transparent helpers), never this class; this module only pushes
 * state into them on enable/disable and once per frame.
 *
 * <p>Stage 1 of {@code mod/docs/optimize-layer-design.md}: the toggles below
 * wire the engine-side state holders up; the actual hooks (mixins on
 * {@code LivingEntityRenderer}, {@code ParticleGroup}, {@code SpawnerRenderer},
 * {@code BlockEntity}, {@code ClientLanguage}, etc.) are added in Stage 3
 * alongside the GL path. The helpers themselves (LRU, LOD math, workers, HUD
 * hysteresis) are already in place under
 * {@code features/module/modules/misc/optimize/{core,entity,particle,hud,block,text}/}.
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
    }

    /** Picks up knob changes made while the module stays on. */
    @Override
    public void onFrame(float d) {
        pushState();
    }

    private void pushState() {
        boolean on = isEnabled();
        OptimizeState.apply(
                on && noGlass.get(),
                on && litePost.get(),
                on && liteSky.get(),
                on && leanHud.get());
        OptimizeToggles.apply(
                on && entityLod.get(),           entityLodDistance.get(),
                on && particleLod.get(),
                on && particleLod.get(),          // particleLight: same gate in Stage 1
                on && particleLod.get(),          // particlePhysics: same gate in Stage 1
                on && spawnerCull.get(),          spawnerDistance.get(),
                on && spawnerTickCache.get(),
                on && blockEntityCache.get(),
                on && preparedTextCache.get(),
                on && microOpts.get());
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
