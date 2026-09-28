/*
 * Ported/adapted from Spatial GUI (https://github.com/tastytrash/Spatial-GUI), MIT License.
 * Copyright (c) tastytrash and Spatial GUI contributors.
 *
 * This file is part of LoVisual and is distributed under LoVisual's own GPLv3 license terms
 * for the modifications made here (see mod/LICENSE and mod/THIRD_PARTY_NOTICES.md); the MIT-licensed
 * portions ported from Spatial GUI remain available under the MIT terms above.
 */
package dev.loki.lovisual.mixins.render.spatialgui;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.loki.lovisual.features.module.modules.visuals.spatialgui.SpatialGui;
import dev.loki.lovisual.render.spatialgui.SpatialGuiCapture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Supplier;

/**
 * Adapted from Spatial-GUI's {@code ScreenExtractor}/{@code TextureTargetManager}: on this Minecraft version the
 * whole 2D GUI pass is a deferred draw list executed once per frame by {@link GuiRenderer#draw()} against an
 * explicit {@link RenderTarget}. Redirecting the currently open screen into a 3D plane therefore only requires
 * pointing that one draw call at {@link SpatialGuiCapture}'s off-screen target instead of the main render
 * target, and cancelling the vanilla draw so nothing shows up as a flat overlay.
 * <p>
 * The vertex data itself is prepared and uploaded earlier in {@code GuiRenderer.render()} (which this mixin does
 * not touch), so this only ever affects where the already-built draw list is executed to.
 */
@Mixin(value = GuiRenderer.class, priority = 900)
public abstract class GuiRendererCaptureMixin {

    @Shadow
    @Final
    private List<?> draws;

    @Shadow
    private int firstDrawIndexAfterBlur;

    @Shadow
    @Final
    private Projection guiProjection;

    @Shadow
    @Final
    private ProjectionMatrixBuffer guiProjectionMatrixBuffer;

    @Shadow
    private void executeDrawRange(Supplier<String> label, RenderTarget target, GpuBufferSlice transform, int startIndex, int endIndex) {
        throw new AssertionError();
    }

    @Inject(method = "draw", at = @At("HEAD"), cancellable = true)
    private void lovisual$captureToSpatialPlane(CallbackInfo ci) {
        if (draws == null || draws.isEmpty()) return;

        SpatialGui module = SpatialGui.getInstance();
        if (module == null || !module.isCapturing()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null) return;
        WindowRenderState windowState = mc.gameRenderer.gameRenderState().windowRenderState;
        if (windowState == null || windowState.guiScale <= 0) return;

        SpatialGuiCapture capture = SpatialGuiCapture.get();
        RenderTarget target = capture.ensure(windowState.width, windowState.height);
        capture.clearTransparent();

        guiProjection.setupOrtho(1000.0F, 11000.0F,
                (float) windowState.width / windowState.guiScale, (float) windowState.height / windowState.guiScale, true);
        RenderSystem.setProjectionMatrix(guiProjectionMatrixBuffer.getBuffer(guiProjection), ProjectionType.ORTHOGRAPHIC);
        GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F));

        int splitIndex = Math.min(firstDrawIndexAfterBlur, draws.size());
        if (splitIndex > 0) {
            executeDrawRange(() -> "spatial gui before blur", target, transform, 0, splitIndex);
        }
        if (draws.size() > firstDrawIndexAfterBlur) {
            RenderSystem.getDevice().createCommandEncoder().clearDepthTexture(target.getDepthTexture(), 0.0D);
            mc.gameRenderer.processBlurEffect();
            executeDrawRange(() -> "spatial gui after blur", target, transform, firstDrawIndexAfterBlur, draws.size());
        }

        ci.cancel();
    }
}
