package dev.loki.lovisual.mixins.accessors.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderDispatcher.class)
public interface EntityRenderManagerAccessor {
    @Invoker("getRenderer")
    EntityRenderer<?, ?> lovisual$invokeGetRenderer(Entity entity);

    @Invoker("submit")
    void lovisual$invokeRender(EntityRenderState renderState,
                                CameraRenderState cameraState,
                                double offsetX,
                                double offsetY,
                                double offsetZ,
                                PoseStack matrices,
                                SubmitNodeCollector queue);
}
