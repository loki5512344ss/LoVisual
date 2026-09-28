/*
 * Ported/adapted from Spatial GUI (https://github.com/tastytrash/Spatial-GUI), MIT License.
 * Copyright (c) tastytrash and Spatial GUI contributors.
 *
 * This file is part of LoVisual and is distributed under LoVisual's own GPLv3 license terms
 * for the modifications made here (see mod/LICENSE and mod/THIRD_PARTY_NOTICES.md); the MIT-licensed
 * portions ported from Spatial GUI remain available under the MIT terms above.
 */
package dev.loki.lovisual.render.spatialgui;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import org.joml.Vector4f;

/**
 * Owns the off-screen {@link RenderTarget} that the currently open {@link net.minecraft.client.gui.screens.Screen}
 * is redirected into when the SpatialGui module is displaying it as a 3D plane instead of a flat overlay.
 * <p>
 * This replaces Spatial-GUI's {@code TextureTargetManager}/{@code ScreenExtractor} pair: on this Minecraft
 * version the GUI is drawn through a deferred {@code GuiRenderer} that executes its draw list against an
 * explicit {@link RenderTarget}, so capturing the screen only requires swapping that target for the duration
 * of one draw call (see {@code GuiRendererCaptureMixin}) instead of hooking a legacy immediate-mode GL pass.
 */
public final class SpatialGuiCapture {
    private static final SpatialGuiCapture INSTANCE = new SpatialGuiCapture();

    private TextureTarget target;
    private int width;
    private int height;

    private SpatialGuiCapture() {
    }

    public static SpatialGuiCapture get() {
        return INSTANCE;
    }

    /**
     * Ensures the capture target exists and matches the requested pixel size. Must be called on the render thread.
     */
    public RenderTarget ensure(int pixelWidth, int pixelHeight) {
        pixelWidth = Math.max(1, pixelWidth);
        pixelHeight = Math.max(1, pixelHeight);
        if (target == null) {
            target = new TextureTarget("LoVisual Spatial GUI", pixelWidth, pixelHeight, true, GpuFormat.RGBA8_UNORM);
            width = pixelWidth;
            height = pixelHeight;
        } else if (width != pixelWidth || height != pixelHeight) {
            target.resize(pixelWidth, pixelHeight);
            width = pixelWidth;
            height = pixelHeight;
        }
        return target;
    }

    public void clearTransparent() {
        if (target == null) return;
        GpuTexture colorTexture = target.getColorTexture();
        if (colorTexture == null) return;
        if (target.useDepth && target.getDepthTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder()
                    .clearColorAndDepthTextures(colorTexture, new Vector4f(0.0F, 0.0F, 0.0F, 0.0F), target.getDepthTexture(), 1.0D);
        } else {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(colorTexture, new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
        }
    }

    public GpuTextureView colorTextureView() {
        return target == null ? null : target.getColorTextureView();
    }

    public boolean isReady() {
        return target != null && target.getColorTextureView() != null;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void close() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
