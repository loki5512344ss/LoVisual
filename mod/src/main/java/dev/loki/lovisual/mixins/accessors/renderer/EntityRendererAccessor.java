package dev.loki.lovisual.mixins.accessors.renderer;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {
    @Invoker("createRenderState")
    EntityRenderState lovisual$invokeCreateRenderState();

    @Invoker("extractRenderState")
    void lovisual$invokeUpdateRenderState(Entity entity, EntityRenderState state, float tickProgress);
}
