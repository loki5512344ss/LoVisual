package dev.loki.lovisual.mixins.optimize.stats;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.render.engine.optimize.profile.DrawStats;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Counts {@code RenderPass.setPipeline} — one per state selection, repeats
 * included — and feeds the pipeline name to the per-frame top list, so F3
 * shows which pipelines a frame spends its draws on. That is the input for
 * the "sort draws by pipeline" idea in
 * {@code mod/docs/optimize-layer-design.md}.
 *
 * <p>Part of Stage 3 step 1 in {@code mod/docs/optimize-layer-design.md}.
 *
 */
@Mixin(targets = "com.mojang.blaze3d.opengl.GlRenderPass")
public abstract class GlRenderPassStatsMixin {

    @Inject(method = "setPipeline", at = @At("HEAD"))
    private void lovisual$countPipeline(RenderPipeline pipeline, CallbackInfo ci) {
        if (pipeline == null || pipeline.getLocation() == null) {
            return;
        }
        DrawStats.pipeline(pipeline.getLocation().getPath());
    }
}
