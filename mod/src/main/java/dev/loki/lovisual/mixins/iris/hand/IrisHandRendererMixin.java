package dev.loki.lovisual.mixins.iris.hand;

import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.render.iris.compat.IrisCompatibilityGuards;
import dev.loki.lovisual.render.iris.scene.IrisSecondHandScene;

@Pseudo
@Mixin(value = HandRenderer.class, remap = false)
public abstract class IrisHandRendererMixin {
    @Inject(method = "renderSolid", at = @At("HEAD"), cancellable = true, remap = false)
    private void lovisual$disableIrisSolidHand(Matrix4fc modelMatrix,
                                                float tickDelta,
                                                Camera camera,
                                                CameraRenderState cameraRenderState,
                                                GameRenderer gameRenderer,
                                                WorldRenderingPipeline pipeline,
                                                CallbackInfo ci) {
        if (IrisCompatibilityGuards.suppressIrisHandRendering() && !IrisSecondHandScene.isRendering()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderTranslucent", at = @At("HEAD"), cancellable = true, remap = false)
    private void lovisual$disableIrisTranslucentHand(Matrix4fc modelMatrix,
                                                      float tickDelta,
                                                      Camera camera,
                                                      CameraRenderState cameraRenderState,
                                                      GameRenderer gameRenderer,
                                                      WorldRenderingPipeline pipeline,
                                                      CallbackInfo ci) {
        if (IrisCompatibilityGuards.suppressIrisHandRendering() && !IrisSecondHandScene.isRendering()) {
            ci.cancel();
        }
    }
}
