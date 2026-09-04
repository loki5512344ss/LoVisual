/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.depth;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.mixininterface.IMsaaTexture;
import dev.loki.lovisual.render.engine.core.LoVisualRenderSystem;

/**
 * Persistent copy of the depth buffers that vanilla composes through its world transparency
 * frame graph. The copy is taken by a vanilla FramePass after transparency composition has
 * been scheduled, so DoF samples the same scene-stage depth that contributed to final world
 * color instead of relying on a single late main depth attachment.
 */
public enum WorldSceneDepth {
    ;
    private static final Stage MAIN = new Stage("main", "lovisual-world-scene-depth-main");
    private static final Stage TRANSLUCENT = new Stage("translucent", "lovisual-world-scene-depth-translucent");
    private static final Stage ITEM_ENTITY = new Stage("item_entity", "lovisual-world-scene-depth-item-entity");
    private static final Stage PARTICLES = new Stage("particles", "lovisual-world-scene-depth-particles");
    private static final Stage WEATHER = new Stage("weather", "lovisual-world-scene-depth-weather");
    private static final Stage CLOUDS = new Stage("clouds", "lovisual-world-scene-depth-clouds");

    private static int width = -1;
    private static int height = -1;
    private static long frameSerial;
    private static boolean valid;

    public static void beginFrame(int w, int h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        frameSerial++;
        valid = false;
        MAIN.reset();
        TRANSLUCENT.reset();
        ITEM_ENTITY.reset();
        PARTICLES.reset();
        WEATHER.reset();
        CLOUDS.reset();
    }

    public static void captureMain(RenderTarget source) {
        valid |= MAIN.capture(source, width, height);
    }

    /** Captures the final PRE_HAND main depth, resolving an MSAA source when necessary. */
    public static boolean captureResolvedMain(RenderTarget source) {
        if (source == null) {
            return false;
        }
        int w = Math.max(1, source.width);
        int h = Math.max(1, source.height);
        if (width != w || height != h) {
            width = w;
            height = h;
        }
        boolean captured = MAIN.capture(source, width, height);
        valid |= captured;
        return captured;
    }

    public static void captureTranslucent(RenderTarget source) {
        valid |= TRANSLUCENT.capture(source, width, height);
    }

    public static void captureItemEntity(RenderTarget source) {
        valid |= ITEM_ENTITY.capture(source, width, height);
    }

    public static void captureParticles(RenderTarget source) {
        valid |= PARTICLES.capture(source, width, height);
    }

    public static void captureWeather(RenderTarget source) {
        valid |= WEATHER.capture(source, width, height);
    }

    public static void captureClouds(RenderTarget source) {
        valid |= CLOUDS.capture(source, width, height);
    }

    public static boolean isValid() {
        return valid && (hasMain() || hasTranslucent() || hasItemEntity() || hasParticles() || hasWeather() || hasClouds());
    }

    public static long frameSerial() {
        return frameSerial;
    }

    public static int width() {
        return width;
    }

    public static int height() {
        return height;
    }

    public static boolean hasMain() {
        return MAIN.hasDepth();
    }

    public static boolean hasTranslucent() {
        return TRANSLUCENT.hasDepth();
    }

    public static boolean hasItemEntity() {
        return ITEM_ENTITY.hasDepth();
    }

    public static boolean hasParticles() {
        return PARTICLES.hasDepth();
    }

    public static boolean hasWeather() {
        return WEATHER.hasDepth();
    }

    public static boolean hasClouds() {
        return CLOUDS.hasDepth();
    }

    public static GpuTextureView mainDepthView() {
        return MAIN.depthView();
    }

    public static GpuTextureView translucentDepthView() {
        return TRANSLUCENT.depthView();
    }

    public static GpuTextureView itemEntityDepthView() {
        return ITEM_ENTITY.depthView();
    }

    public static GpuTextureView particlesDepthView() {
        return PARTICLES.depthView();
    }

    public static GpuTextureView weatherDepthView() {
        return WEATHER.depthView();
    }

    public static GpuTextureView cloudsDepthView() {
        return CLOUDS.depthView();
    }

    public static void reset() {
        valid = false;
        MAIN.reset();
        TRANSLUCENT.reset();
        ITEM_ENTITY.reset();
        PARTICLES.reset();
        WEATHER.reset();
        CLOUDS.reset();
    }

    private static final class Stage {
        private final String stageName;
        private final String framebufferName;
        private TextureTarget depthCopy;
        private boolean captured;

        private Stage(String stageName, String framebufferName) {
            this.stageName = stageName;
            this.framebufferName = framebufferName;
        }

        private boolean capture(RenderTarget source, int w, int h) {
            captured = false;
            if (source == null || source.getDepthTexture() == null || w <= 0 || h <= 0) {
                return false;
            }

            try {
                depthCopy = LoVisualRenderSystem.resources().persistentFramebuffer(
                        framebufferName,
                        w,
                        h,
                        true,
                        "WorldSceneDepth:" + stageName
                );
                if (depthCopy == null || depthCopy.getDepthTexture() == null) {
                    return false;
                }

                boolean msaa = source.getDepthTexture() instanceof IMsaaTexture texture && texture.lovisual$isMsaa();
                if (msaa) {
                    if (LoVisualRenderSystem.rhi().msaa().supportsDepthSnapshotResolve()) {
                        captured = LoVisualRenderSystem.rhi().msaa().resolveDepthSnapshot(
                                source.getDepthTextureView(),
                                depthCopy.getDepthTextureView()
                        );
                    } else {
                        captured = LoVisualRenderSystem.rhi().msaa().resolve(source, depthCopy, false, true);
                    }
                } else {
                    depthCopy.copyDepthFrom(source);
                    captured = true;
                }
            } catch (Throwable ignored) {
                captured = false;
            }
            return captured;
        }

        private boolean hasDepth() {
            return captured && depthCopy != null && depthCopy.getDepthTextureView() != null;
        }

        private GpuTextureView depthView() {
            return hasDepth() ? depthCopy.getDepthTextureView() : null;
        }

        private void reset() {
            captured = false;
        }
    }
}
