package dev.loki.lovisual.mixins.optimize.hud;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.misc.optimize.Optimize;
import dev.loki.lovisual.render.engine.optimize.profile.DrawStats;
import dev.loki.lovisual.render.engine.optimize.profile.FrameBudget;
import dev.loki.lovisual.render.engine.optimize.profile.FrameProfiler;
import dev.loki.lovisual.render.engine.optimize.profile.OptimizePropertyBridge;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.ArrayList;
import java.util.List;

/**
 * F3 debug overlay for the Optimize layer — Stage 2 of
 * {@code mod/docs/optimize-layer-design.md}.
 *
 * <p>Injects three groups of lines into the F3 text list, prepended before
 * the existing LoVisual engine block:
 * <ul>
 *   <li>Optimize status — which toggles are on, plus the budget mode + target FPS.</li>
 *   <li>Frame profiler — EMA-averaged per-section timings (entities/particles/HUD/effects/text).</li>
 *   <li>Property overrides — when any {@code -Dlovisual.opt.*} is set, list which ones.</li>
 * </ul>
 *
 * <p>The hook mirrors the existing {@code hud.DebugHudMixin}'s pattern: it
 * {@code ModifyArg}s the {@code extractLines} call inside
 * {@code DebugScreenOverlay.extractRenderState} and replaces the text list.
 * Both mixins return a new list with their own lines prepended; whichever runs
 * last sees the previous one's output in its input list, so both blocks appear
 * in F3.
 */
@Mixin(DebugScreenOverlay.class)
public abstract class OptimizeDebugHudMixin {

    @ModifyArg(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/components/DebugScreenOverlay;extractLines(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Ljava/util/List;Z)V",
                    ordinal = 0
            ),
            index = 1
    )
    private List<String> lovisual$addOptimizeInfo(List<String> list) {
        if (list == null) return list;
        Optimize module = Modules.get(Optimize.class);
        if (module == null || !module.isEnabled()) {
            return list;
        }

        List<String> out = new ArrayList<>(list.size() + 16);
        out.add("§cLoVisual Optimize");
        out.add(String.format("toggles: %s", togglesSummary()));
        out.add(String.format("budget: %s @ %d fps (scale %.2f)",
                FrameBudget.mode(),
                FrameBudget.targetFps(),
                FrameBudget.scale()));

        FrameProfiler.Snapshot snap = FrameProfiler.snapshot();
        if (snap.frameMs() > 0) {
            out.add(String.format("frame: %.2f ms (%.0f fps)",
                    snap.frameMs(),
                    1000.0 / Math.max(0.01, snap.frameMs())));
            if (hasSectionTimings(snap)) {
                out.add(String.format("  entities=%.2fms particles=%.2fms hud=%.2fms effects=%.2fms text=%.2fms",
                        snap.entityMs(),
                        snap.particleMs(),
                        snap.hudMs(),
                        snap.effectMs(),
                        snap.textMs()));
            }
        }

        DrawStats.Snapshot counters = DrawStats.snapshot();
        if (counters.frames() > 0) {
            out.add(String.format("draws: %d, pipelines: %d, fbo binds: %d (avg/frame)",
                    counters.draws(),
                    counters.pipelines(),
                    counters.binds()));
            if (!counters.top().isEmpty()) {
                out.add("  top: " + String.join(", ", counters.top()));
            }
        }

        if (OptimizePropertyBridge.hasOverrides()) {
            out.add("props: " + OptimizePropertyBridge.lastResolved());
        }

        out.add("");
        out.addAll(list);
        return out;
    }

    /** Sections are only reported once something opens them — a row of zeros is noise. */
    private static boolean hasSectionTimings(FrameProfiler.Snapshot snap) {
        return snap.entityMs() > 0
                || snap.particleMs() > 0
                || snap.hudMs() > 0
                || snap.effectMs() > 0
                || snap.textMs() > 0;
    }

    private static String togglesSummary() {
        StringBuilder sb = new StringBuilder();
        if (OptimizeToggles.entityLod())        append(sb, "entity_lod@" + OptimizeToggles.entityLodDistance());
        if (OptimizeToggles.particleLod())     append(sb, "particle_lod");
        if (OptimizeToggles.spawnerCull())     append(sb, "spawner_cull@" + OptimizeToggles.spawnerDistance());
        if (OptimizeToggles.spawnerTickCache()) append(sb, "spawner_tick_cache");
        if (OptimizeToggles.blockEntityCache()) append(sb, "block_entity_cache");
        if (OptimizeToggles.preparedTextCache()) append(sb, "prepared_text_cache");
        if (OptimizeToggles.microOpts())       append(sb, "micro_opts");
        return sb.length() == 0 ? "none" : sb.toString();
    }

    private static void append(StringBuilder out, String knob) {
        if (out.length() > 0) out.append(", ");
        out.append(knob);
    }
}
