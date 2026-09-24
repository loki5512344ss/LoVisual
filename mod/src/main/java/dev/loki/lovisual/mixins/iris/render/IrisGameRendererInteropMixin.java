package dev.loki.lovisual.mixins.iris.render;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.render.iris.state.IrisLoVisualFrameHooks;
import dev.loki.lovisual.render.iris.scene.IrisSecondHandScene;

@Mixin(value = GameRenderer.class, priority = 900)
public abstract class IrisGameRendererInteropMixin {
    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V",
                    shift = At.Shift.BEFORE
            )
    )
    private void lovisual$renderIrisHandInSecondScene(DeltaTracker tickCounter, CallbackInfo ci) {
        IrisSecondHandScene.finalizeWorld();
        IrisLoVisualFrameHooks.renderAfterIrisFinalization();
    }

}
