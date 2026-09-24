package dev.loki.lovisual.mixins.accessors.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntityRenderState.class)
public interface LivingEntityRenderStateAccessor {

    @Accessor("bodyRot")
    void lovisual$setBodyYaw(float bodyYaw);

    @Accessor("yRot")
    void lovisual$setRelativeHeadYaw(float relativeHeadYaw);

    @Accessor("xRot")
    void lovisual$setPitch(float pitch);
}
