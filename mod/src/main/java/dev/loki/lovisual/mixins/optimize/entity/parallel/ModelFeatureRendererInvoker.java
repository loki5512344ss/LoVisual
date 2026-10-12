package dev.loki.lovisual.mixins.optimize.entity.parallel;

import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Stage 3 step 8: the vanilla per-model build, invoked as the fallback for
 * submits the parallel path could not take (wrong format, decals, frozen
 * animations, raw-vertex consumer unavailable).
 */
@Mixin(ModelFeatureRenderer.class)
public interface ModelFeatureRendererInvoker {

    @Invoker("prepareModel")
    <S> void lovisual$prepareModel(ModelFeatureRenderer.Submit<S> submit);
}
