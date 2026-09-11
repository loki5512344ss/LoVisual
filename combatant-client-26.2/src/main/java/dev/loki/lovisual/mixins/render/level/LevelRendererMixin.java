/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixins.render.level;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.scene.BlockHighlight;
import dev.loki.lovisual.features.module.modules.visuals.scene.ReimaginedVisual;
import dev.loki.lovisual.features.module.modules.visuals.weather.ShaderSky;
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;
import dev.loki.lovisual.mixins.accessors.renderer.GameRendererAccessor;
import dev.loki.lovisual.mixins.accessors.world.LevelRendererAccessor;
import dev.loki.lovisual.render.engine.core.system.LoVisualWorldMatrices;
import dev.loki.lovisual.render.engine.depth.PreTranslucentDepth;
import dev.loki.lovisual.render.engine.depth.WorldSceneDepth;
import dev.loki.lovisual.render.iris.runtime.IrisRuntime;
import dev.loki.lovisual.render.sky.CustomSkyboxRenderer;

@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {

    @Shadow
    @Final
    private LevelTargetBundle targets;

    @WrapOperation(
            method = "addSkyPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"
            )
    )
    private RenderTarget lovisual$stableSkyRendererTarget(GameRenderer instance, Operation<RenderTarget> original) {
        return ((GameRendererAccessor) instance).lovisual$getMainRenderTargetRaw();
    }

    @Unique
    private static void lovisual$dependsOn(FramePass pass, ResourceHandle<RenderTarget> handle) {
        if (pass != null && handle != null) {
            pass.reads(handle);
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private static ResourceHandle<RenderTarget> lovisual$asRenderTargetHandle(ResourceHandle<?> handle) {
        return (ResourceHandle<RenderTarget>) handle;
    }

    @Unique
    private static RenderTarget lovisual$getFramebuffer(ResourceHandle<RenderTarget> handle) {
        if (handle == null) {
            return null;
        }
        try {
            return handle.get();
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Unique
    private static boolean lovisual$needsWorldSceneDepthCapture() {
        ReimaginedVisual module = Modules.get(ReimaginedVisual.class);
        return module != null && module.needsWorldSceneDepthCapture();
    }

    @Unique
    private static boolean shouldSkipSky(net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState) {
        FogType submersion = cameraRenderState.fogType;
        if (submersion == FogType.POWDER_SNOW || submersion == FogType.LAVA) {
            return true;
        }
        return cameraRenderState.entityRenderState != null
                && cameraRenderState.entityRenderState.doesMobEffectBlockSky;
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void lovisual$captureIrisModelView(
            GraphicsResourceAllocator allocator,
            DeltaTracker tickCounter,
            boolean renderBlockOutline,
            net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState,
            org.joml.Matrix4fc positionMatrix,
            GpuBufferSlice fogBuffer,
            org.joml.Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci
    ) {
        if (positionMatrix == null) {
            return;
        }

        if (LoVisualWorldMatrices.isValid()) {
            LoVisualWorldMatrices.capturePosition(
                    positionMatrix,
                    cameraRenderState != null && cameraRenderState.pos != null
                            ? cameraRenderState.pos
                            : null
            );
            return;
        }

        if (cameraRenderState != null && cameraRenderState.projectionMatrix != null) {
            LoVisualWorldMatrices.capture(
                    positionMatrix,
                    cameraRenderState.projectionMatrix,
                    cameraRenderState.projectionMatrix,
                    cameraRenderState.pos
            );
        }
    }

    @Inject(
            method = "submitBlockOutline",
            at = @At("HEAD"),
            cancellable = true
    )
    private void lovisual$disableVanillaOutline(PoseStack matrices,
                                                 SubmitNodeCollector consumers,
                                                 LevelRenderState renderState,
                                                 CallbackInfo ci) {
        BlockHighlight blockHighlight = Modules.get(BlockHighlight.class);
        if (blockHighlight != null && blockHighlight.isOutlineActive()) {
            ci.cancel();
        }
    }

    @Inject(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/LevelRenderer;addMainPass(Lcom/mojang/blaze3d/framegraph/FrameGraphBuilder;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher$PreparedFrame;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/client/renderer/state/level/LevelRenderState;Lnet/minecraft/util/profiling/ProfilerFiller;Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;)V",
                    shift = At.Shift.AFTER
            )
    )
    private void lovisual$captureWorldSceneDepth(
            GraphicsResourceAllocator allocator,
            DeltaTracker tickCounter,
            boolean renderBlockOutline,
            net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState,
            org.joml.Matrix4fc positionMatrix,
            GpuBufferSlice fogBuffer,
            org.joml.Vector4f fogColor,
            boolean renderSky,
            CallbackInfo ci,
            @Local FrameGraphBuilder frameGraphBuilder
    ) {
        if (IrisRuntime.isShaderpackRendererActive()) {
            return;
        }
        if (frameGraphBuilder == null || targets == null || targets.main == null) {
            return;
        }
        if (!lovisual$needsWorldSceneDepthCapture()) {
            WorldSceneDepth.reset();
            return;
        }

        ResourceHandle<RenderTarget> main = targets.main;
        ResourceHandle<RenderTarget> translucent = targets.translucent;
        ResourceHandle<RenderTarget> itemEntity = targets.itemEntity;
        ResourceHandle<RenderTarget> particles = targets.particles;
        ResourceHandle<RenderTarget> weather = targets.weather;

        FramePass pass = frameGraphBuilder.addPass("lovisual_scene_depth_capture");
        lovisual$dependsOn(pass, main);
        lovisual$dependsOn(pass, translucent);
        lovisual$dependsOn(pass, itemEntity);
        lovisual$dependsOn(pass, particles);
        lovisual$dependsOn(pass, weather);
        pass.disableCulling();
        pass.executes(() -> {
            RenderTarget mainFramebuffer = lovisual$getFramebuffer(main);
            Minecraft client = Minecraft.getInstance();
            RenderTarget fallbackFramebuffer = client != null ? client.gameRenderer.mainRenderTarget() : null;
            int width = mainFramebuffer != null ? mainFramebuffer.width : fallbackFramebuffer != null ? fallbackFramebuffer.width : 1;
            int height = mainFramebuffer != null ? mainFramebuffer.height : fallbackFramebuffer != null ? fallbackFramebuffer.height : 1;

            WorldSceneDepth.beginFrame(width, height);
            WorldSceneDepth.captureMain(mainFramebuffer);
            WorldSceneDepth.captureTranslucent(lovisual$getFramebuffer(translucent));
            WorldSceneDepth.captureItemEntity(lovisual$getFramebuffer(itemEntity));
            WorldSceneDepth.captureParticles(lovisual$getFramebuffer(particles));
            WorldSceneDepth.captureWeather(lovisual$getFramebuffer(weather));
        });
    }

    @Inject(method = "addSkyPass", at = @At("HEAD"), cancellable = true)
    private void lovisual$renderSky(FrameGraphBuilder frameGraphBuilder, net.minecraft.client.renderer.state.level.CameraRenderState cameraRenderState, GpuBufferSlice fog, CallbackInfo ci) {
        if (IrisRuntime.isModLoaded()) return;
        if (shouldSkipSky(cameraRenderState)) return;

        LevelRendererAccessor accessor = (LevelRendererAccessor) this;
        SkyRenderState sky = accessor.lovisual$getWorldRenderState().skyRenderState;
        if (sky == null || sky.skybox != DimensionType.Skybox.OVERWORLD) return;

        WorldTweaks worldTweaks = Modules.get(WorldTweaks.class);
        if (worldTweaks != null) {
            worldTweaks.applySkyOverrides(sky);
        }

        ReimaginedVisual module = Modules.get(ReimaginedVisual.class);
        ShaderSky skyModule = Modules.get(ShaderSky.class);
        boolean needsCustomSky = (module != null && module.isEnabled())
                || (skyModule != null && skyModule.isEnabled());
        if (!needsCustomSky) return;

        Minecraft client = Minecraft.getInstance();
        net.minecraft.client.Camera camera = client != null && client.gameRenderer != null
                ? client.gameRenderer.mainCamera()
                : null;
        if (camera == null) return;

        FramePass pass = frameGraphBuilder.addPass("lovisual_sky");
        targets.main = pass.readsAndWrites(targets.main);
        pass.executes(() -> CustomSkyboxRenderer.render(camera, fog, sky, accessor.lovisual$getSkyRendering()));
        ci.cancel();
    }

    @Inject(
            method = "lambda$addMainPass$0",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/chunk/ChunkSectionsToRender;renderGroup(Lnet/minecraft/client/renderer/chunk/ChunkSectionLayerGroup;Lcom/mojang/blaze3d/textures/GpuSampler;)V",
                    ordinal = 1,
                    shift = At.Shift.BEFORE
            ),
            remap = false
    )
    private void lovisual$capturePreWaterScene(
            GpuBufferSlice fogBuffer,
            LevelRenderState worldRenderState,
            net.minecraft.util.profiling.ProfilerFiller profiler,
            net.minecraft.client.renderer.chunk.ChunkSectionsToRender chunkSections,
            com.mojang.blaze3d.resource.ResourceHandle<?> entityOutlineHandle,
            net.minecraft.client.renderer.feature.FeatureRenderDispatcher.PreparedFrame preparedFrame,
            com.mojang.blaze3d.resource.ResourceHandle<?> translucentFramebufferHandle,
            com.mojang.blaze3d.resource.ResourceHandle<?> mainFramebufferHandle,
            com.mojang.blaze3d.resource.ResourceHandle<?> itemEntityFramebufferHandle,
            com.mojang.blaze3d.resource.ResourceHandle<?> particlesFramebufferHandle,
            CallbackInfo ci
    ) {
        if (IrisRuntime.isShaderpackRendererActive()) {
            return;
        }
        if (!PreTranslucentDepth.takeRequested()) {
            PreTranslucentDepth.reset();
            return;
        }
        RenderTarget mainFramebuffer = lovisual$getFramebuffer(lovisual$asRenderTargetHandle(mainFramebufferHandle));
        if (mainFramebuffer == null) {
            Minecraft mc = Minecraft.getInstance();
            mainFramebuffer = mc != null && mc.gameRenderer != null ? mc.gameRenderer.mainRenderTarget() : null;
        }
        PreTranslucentDepth.captureFrom(mainFramebuffer);
    }
}
