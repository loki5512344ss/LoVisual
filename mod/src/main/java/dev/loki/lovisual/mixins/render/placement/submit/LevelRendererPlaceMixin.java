package dev.loki.lovisual.mixins.render.placement.submit;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.placeanimation.PlaceAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PlaceAnimation: анимируемые блоки отправляются на отрисовку вместе с сущностями. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererPlaceMixin {

    @Inject(method = "submitEntities(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/client/renderer/SubmitNodeCollector;)V",
            at = @At("TAIL"))
    private void lovisual$submitPlacingBlocks(PoseStack poseStack, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
        PlaceAnimation module = Modules.get(PlaceAnimation.class);
        if (module == null) return;
        module.submit(poseStack, collector, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
}
