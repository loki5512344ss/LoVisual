package dev.loki.lovisual.mixins.render.placement.submit;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.placeanimation.PlaceAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** PlaceAnimation: блок-сущности (сундуки, таблички) анимируются преобразованием позы, а не подменой меша. */
@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityPlaceMixin {

    @Inject(method = "submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"))
    private void lovisual$animatePlacingBlockEntity(BlockEntityRenderState renderState, PoseStack poseStack,
                                                    SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
        PlaceAnimation module = Modules.get(PlaceAnimation.class);
        if (module == null) return;
        module.applyBlockEntity(renderState.blockPos, poseStack, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }
}
