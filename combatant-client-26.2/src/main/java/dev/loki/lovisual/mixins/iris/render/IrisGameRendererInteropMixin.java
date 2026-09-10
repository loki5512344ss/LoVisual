/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

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
