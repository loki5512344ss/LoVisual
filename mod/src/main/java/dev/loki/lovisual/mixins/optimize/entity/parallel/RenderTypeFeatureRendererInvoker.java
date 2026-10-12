package dev.loki.lovisual.mixins.optimize.entity.parallel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Stage 3 step 8: the per-render-type vertex builder the parallel emit hands
 * its raw vertices to — one builder call per model, exactly as vanilla's
 * prepareModel would make.
 */
@Mixin(RenderTypeFeatureRenderer.class)
public interface RenderTypeFeatureRendererInvoker {

    @Invoker("getVertexBuilder")
    VertexConsumer lovisual$vertexBuilder(RenderType renderType);
}
