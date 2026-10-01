package dev.loki.lovisual.mixins.render.main;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.render.gamerenderer.GameRendererProjectionHelper;
import dev.loki.lovisual.render.gamerenderer.GameRendererWorldEngine;
import dev.loki.lovisual.render.gamerenderer.GameRendererWorldPost;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.BetterButtons;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.camera.AspectRatio;
import dev.loki.lovisual.features.module.modules.visuals.camera.Freecam;
import dev.loki.lovisual.features.module.modules.visuals.camera.ViewModel;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.Chams;
import dev.loki.lovisual.mixins.accessors.renderer.GameRendererAccessor;
import dev.loki.lovisual.render.engine.core.system.LoVisualWorldMatrices;
import dev.loki.lovisual.render.engine.msaa.MsaaWorldTarget;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.iris.state.IrisLoVisualFrameHooks;
import dev.loki.lovisual.render.iris.scene.IrisFinalizedSceneRenderer;
import dev.loki.lovisual.util.screen.ClientScreen;

/**
 * Hook-only mixin: engine bodies live in handlers/ (GameRendererWorldEngine,
 * GameRendererWorldPost, GameRendererWorldDebug, GameRendererProjectionHelper).
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin
        implements IrisFinalizedSceneRenderer, GameRendererProjectionHelper.BobInvoker, GameRendererWorldEngine.Host {

    @Unique
    private final GameRendererProjectionHelper lovisual$projectionHelper = new GameRendererProjectionHelper();
    @Unique
    private final GameRendererWorldEngine lovisual$worldEngine = new GameRendererWorldEngine();
    @Unique
    private final GameRendererWorldPost lovisual$worldPost = new GameRendererWorldPost(lovisual$worldEngine);
    // Per-frame scratch (GameRenderer is a singleton; consumers copy by value).
    @Unique
    private final PoseStack lovisual$effectMatrices = new PoseStack();
    @Unique
    private final Matrix4f lovisual$screenProjectionScratch = new Matrix4f();

    @Shadow
    @Final
    private Minecraft minecraft;
    @Shadow
    @Final
    private Camera mainCamera;
    @Shadow
    @Final
    private GameRenderState gameRenderState;
    @Shadow
    private float spinningEffectTime;
    @Shadow
    private float spinningEffectSpeed;

    @Shadow
    protected abstract void bobView(CameraRenderState cameraRenderState, PoseStack matrices);

    @Shadow
    protected abstract void bobHurt(CameraRenderState cameraRenderState, PoseStack matrices);

    @Unique
    @Override
    public Minecraft lovisual$minecraft() {
        return minecraft;
    }

    @Unique
    @Override
    public Camera lovisual$mainCamera() {
        return mainCamera;
    }

    @Unique
    @Override
    public GameRendererProjectionHelper lovisual$projection() {
        return lovisual$projectionHelper;
    }

    @Unique
    @Override
    public float lovisual$spinningTime() {
        return spinningEffectTime;
    }

    @Unique
    @Override
    public float lovisual$spinningSpeed() {
        return spinningEffectSpeed;
    }

    @Unique
    @Override
    public void lovisual$bobHurt(CameraRenderState cameraRenderState, PoseStack matrices) {
        bobHurt(cameraRenderState, matrices);
    }

    @Unique
    @Override
    public void lovisual$bobView(CameraRenderState cameraRenderState, PoseStack matrices) {
        bobView(cameraRenderState, matrices);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void lovisual$skipTickWithoutPlayer(CallbackInfo ci) {
        if (minecraft != null && minecraft.level != null && minecraft.player == null) {
            ci.cancel();
        }
    }

    @Inject(method = "extract(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("HEAD"))
    private void lovisual$beginDeferred2DExtract(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        Renderer2D.beginDeferredExtractFrame();
    }

    @Inject(method = "extract(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("TAIL"))
    private void lovisual$endDeferred2DExtract(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        Renderer2D.endDeferredExtractFrame();
    }

    @Redirect(
            method = "renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"
            )
    )
    private void lovisual$viewModel$replaceHandBobbing(GameRenderer instance,
                                                        CameraRenderState cameraRenderState,
                                                        PoseStack matrices,
                                                        CameraRenderState originalCameraRenderState,
                                                        float tickDelta,
                                                        Matrix4fc positionMatrix) {
        ViewModel viewModel = Modules.get(ViewModel.class);
        if (viewModel != null && viewModel.smoothRunLoweringEnabled()) {
            if (minecraft != null && minecraft.getCameraEntity() instanceof AbstractClientPlayer player) {
                float movement = player.avatarState().getInterpolatedBob(tickDelta);
                float yOffset = -Mth.clamp(movement, 0.0f, 1.0f) * viewModel.getSmoothRunLoweringAmount();
                if (Math.abs(yOffset) > 0.0001f) {
                    matrices.translate(0.0f, yOffset, 0.0f);
                }
            }
            return;
        }

        ((GameRendererAccessor) instance).invokeBobView(cameraRenderState, matrices);
    }

    @Inject(method = "extractCamera", at = @At("TAIL"))
    private void lovisual$modifyAspect(DeltaTracker tickCounter, float tickDelta, float fov, CallbackInfo ci) {
        lovisual$projectionHelper.applyAspect(gameRenderState.levelRenderState.cameraRenderState);
    }

    @Inject(method = "extractCamera", at = @At("TAIL"))
    private void lovisual$captureProjectionFor2DOverlays(DeltaTracker tickCounter, float tickDelta, float fov, CallbackInfo ci) {
        if (gameRenderState == null || gameRenderState.levelRenderState == null) return;
        CameraRenderState cameraRenderState = gameRenderState.levelRenderState.cameraRenderState;
        if (cameraRenderState == null || cameraRenderState.viewRotationMatrix == null || cameraRenderState.projectionMatrix == null) return;

        lovisual$effectMatrices.setIdentity();
        Matrix4f renderProjection = lovisual$projectionHelper.buildWorldProjection(
                mainCamera, fov, lovisual$effectMatrices, tickDelta,
                spinningEffectTime, spinningEffectSpeed, minecraft, this);
        AspectRatio.applyToProjection(renderProjection);
        Matrix4f screenProjection = lovisual$screenProjectionScratch.set(cameraRenderState.projectionMatrix);
        LoVisualWorldMatrices.capture(
                cameraRenderState.viewRotationMatrix,
                renderProjection,
                screenProjection,
                cameraRenderState.pos != null ? cameraRenderState.pos : mainCamera.position()
        );
    }

    @Inject(method = "mainRenderTarget", at = @At("HEAD"), cancellable = true)
    private void lovisual$overrideWorldFramebuffer(CallbackInfoReturnable<RenderTarget> cir) {
        RenderTarget msaa = MsaaWorldTarget.getFramebufferOverride();
        if (msaa != null) {
            cir.setReturnValue(msaa);
        }
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void lovisual$beginWorldMsaa(DeltaTracker tickCounter, CallbackInfo ci) {
        IrisLoVisualFrameHooks.beginRenderLevel(tickCounter);
        LoVisualWorldMatrices.reset();
        MsaaWorldTarget.begin(minecraft, MainConfig.get().getMsaa3dSamples(), lovisual$projectionHelper.needsResolvedMainDepth());
    }

    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void lovisual$endIrisFrameHooks(DeltaTracker tickCounter, CallbackInfo ci) {
        IrisLoVisualFrameHooks.endRenderLevel(tickCounter);
    }

    @WrapOperation(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/ProjectionMatrixBuffer;getBuffer(Lorg/joml/Matrix4f;)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
            )
    )
    private GpuBufferSlice lovisual$captureFullWorldMatrices(ProjectionMatrixBuffer instance,
                                                               Matrix4f renderProjectionMatrix,
                                                               Operation<GpuBufferSlice> original) {
        if (renderProjectionMatrix != null
                && gameRenderState != null
                && gameRenderState.levelRenderState != null
                && gameRenderState.levelRenderState.cameraRenderState != null
                && gameRenderState.levelRenderState.cameraRenderState.viewRotationMatrix != null
                && gameRenderState.levelRenderState.cameraRenderState.projectionMatrix != null) {
            CameraRenderState cameraRenderState = gameRenderState.levelRenderState.cameraRenderState;
            LoVisualWorldMatrices.capture(
                    cameraRenderState.viewRotationMatrix,
                    renderProjectionMatrix,
                    cameraRenderState.projectionMatrix,
                    cameraRenderState.pos != null ? cameraRenderState.pos : mainCamera.position()
            );
        }
        return original.call(instance, renderProjectionMatrix);
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE_STRING",
                    target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V",
                    args = "ldc=hand",
                    shift = At.Shift.AFTER
            )
    )
    private void lovisual$postProcess(DeltaTracker tickCounter, CallbackInfo ci) {
        lovisual$worldPost.preHand(this, tickCounter);
    }

    @Override
    public void lovisual$renderAfterIrisFinalization(DeltaTracker tickCounter) {
        lovisual$worldPost.preHand(this, tickCounter);
    }

    @Inject(
            method = "renderLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void lovisual$postProcessAfterHand(DeltaTracker tickCounter, CallbackInfo ci) {
        lovisual$worldPost.afterHand(this, tickCounter);
    }

    @Inject(method = "renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V", at = @At("HEAD"), cancellable = true)
    private void freecam$hand(CameraRenderState cameraRenderState, float tickDelta, Matrix4fc positionMatrix, CallbackInfo ci) {
        Freecam fc = Modules.get(Freecam.class);
        if (fc != null && fc.isEnabled() && !fc.renderHand()) {
            ci.cancel();
        }
    }

    @WrapOperation(
            method = "renderItemInHand(Lnet/minecraft/client/renderer/state/level/CameraRenderState;FLorg/joml/Matrix4fc;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;renderAllFeatures(Lnet/minecraft/client/renderer/SubmitNodeStorage;)V"
            )
    )
    private void lovisual$renderChamsHandPreparedFrame(FeatureRenderDispatcher dispatcher,
                                                        SubmitNodeStorage storage,
                                                        Operation<Void> original,
                                                        CameraRenderState cameraRenderState,
                                                        float tickDelta,
                                                        Matrix4fc positionMatrix) {
        Chams module = Modules.get(Chams.class);
        if (module != null && module.renderPreparedHandScene(dispatcher, storage)) {
            return;
        }
        original.call(dispatcher, storage);
    }

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("TAIL"))
    private void lovisual$flushQueuedButtons(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || ClientScreen.current() == null) return;
        if (!BetterButtons.hasPending()) {
            BetterButtons.renderTooltip();
            return;
        }
        BetterButtons.flush();
        BetterButtons.renderTooltip();
    }

    @Inject(
            method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V",
                    shift = At.Shift.BEFORE
            )
    )
    private void lovisual$captureWorldForHudGlass(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        lovisual$worldPost.beforeVanillaGui(minecraft, tickCounter);
    }

    @Inject(
            method = "render(Lnet/minecraft/client/DeltaTracker;Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/render/GuiRenderer;render()V",
                    shift = At.Shift.AFTER
            )
    )
    private void lovisual$debug2dImmediateAfterGui(DeltaTracker tickCounter, boolean tick, CallbackInfo ci) {
        lovisual$worldPost.afterVanillaGui(tickCounter);
    }
}
