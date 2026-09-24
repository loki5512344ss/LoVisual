package dev.loki.lovisual.mixins.accessors.world;

import com.mojang.blaze3d.textures.GpuSampler;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("levelRenderState")
    LevelRenderState lovisual$getWorldRenderState();

    @Accessor("skyRenderer")
    SkyRenderer lovisual$getSkyRendering();

    @Accessor("submitNodeStorage")
    SubmitNodeStorage lovisual$getEntityRenderCommandQueue();

    @Accessor("featureRenderDispatcher")
    FeatureRenderDispatcher lovisual$getEntityRenderDispatcher();

    @Accessor("renderBuffers")
    RenderBuffers lovisual$getRenderBuffers();

    @Accessor("renderBuffers")
    void lovisual$setRenderBuffers(RenderBuffers renderBuffers);

    @Accessor("chunkLayerSampler")
    GpuSampler lovisual$getTerrainSampler();

    @Accessor("chunkLayerSampler")
    void lovisual$setTerrainSampler(GpuSampler terrainSampler);

    @Invoker("submitEntities")
    void lovisual$invokeSubmitEntities(com.mojang.blaze3d.vertex.PoseStack poseStack,
                                        LevelRenderState levelRenderState,
                                        SubmitNodeCollector collector);

}
