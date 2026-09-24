package dev.loki.lovisual.mixins.iris.render;

import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.irisshaders.iris.targets.RenderTargets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.render.engine.depth.PreTranslucentDepth;
import dev.loki.lovisual.render.iris.state.IrisLoVisualFrameHooks;
import dev.loki.lovisual.render.iris.scene.IrisSceneDepth;

@Pseudo
@Mixin(value = IrisRenderingPipeline.class, remap = false)
public abstract class IrisRenderingPipelineFinalizeMixin {
    @Shadow
    @Final
    private RenderTargets renderTargets;

    @Inject(method = "beginLevelRendering", at = @At("HEAD"), remap = false)
    private void lovisual$resetIrisSceneDepth(CallbackInfo ci) {
        IrisSceneDepth.resetFrame();
    }

    @Inject(method = "beginTranslucents", at = @At("HEAD"), remap = false)
    private void lovisual$capturePreTranslucentDepth(CallbackInfo ci) {
        if (PreTranslucentDepth.takeRequested()) {
            PreTranslucentDepth.capture();
        } else {
            PreTranslucentDepth.reset();
        }
    }

    @Inject(method = "finalizeLevelRendering", at = @At("HEAD"), remap = false)
    private void lovisual$captureIrisSceneBeforeFinalPass(CallbackInfo ci) {
        IrisSceneDepth.capture(renderTargets);
    }

    @Inject(method = "finalizeLevelRendering", at = @At("TAIL"), remap = false)
    private void lovisual$renderAfterIrisFinalPass(CallbackInfo ci) {
        IrisLoVisualFrameHooks.renderAfterIrisFinalization();
    }

    @Inject(method = "destroy", at = @At("HEAD"), remap = false)
    private void lovisual$shutdownDepth(CallbackInfo ci) {
        IrisSceneDepth.shutdown();
    }
}
