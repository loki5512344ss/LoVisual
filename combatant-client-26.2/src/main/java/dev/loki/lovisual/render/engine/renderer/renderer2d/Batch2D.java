/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.renderer2d;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.ui.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.UiRenderDispatcher;

/**
 * Shared batch scope and scratch buffers for the Renderer2D helper backends.
 *
 * <p>{@link #open} starts an auto batch (only if none is active yet) and fetches the
 * batch for the requested type; {@link #close} flushes only when this call chain
 * actually started the batch. Nesting is handled with a depth counter, so helpers
 * may call other drawing helpers freely.</p>
 */
public final class Batch2D {
    /** Render-thread scratch buffers shared by the helper backends. */
    public static final float[] CORNER_RADII = new float[4];
    public static final double[] RECT_SHAPE = new double[256];
    public static final double[] CONNECTOR = new double[64];
    public static final double[] CONNECTOR_ANCHOR = new double[4];
    public static final int[] POLY_INDEX = new int[64];
    public static final int[] POLY_VERTEX = new int[64];
    public static final double[] PROGRESS_SHAPE = new double[256];
    public static final int[] GRADIENT = new int[4];

    private static final int[] RGBA = new int[4];
    private static final float[] NORM = new float[4];

    private static int depth;
    private static boolean rootAuto;

    private Batch2D() {
    }

    public static DrawBatch open(Renderer2D r, UiBatchType type) {
        return open(r, type, null, null);
    }

    public static DrawBatch open(Renderer2D r, UiBatchType type, GpuTextureView view, GpuSampler sampler) {
        beginScope();
        return finish(r, Renderer2D.UI_BATCHER.getOrCreate(type, view, sampler));
    }

    public static DrawBatch openBlur(Renderer2D r, UiBatchType type, GpuTextureView view, GpuSampler sampler,
                                     Renderer2D.BlurQuality quality, float offsetPx) {
        beginScope();
        return finish(r, Renderer2D.UI_BATCHER.getOrCreateBlur(type, view, sampler, quality, offsetPx));
    }

    public static DrawBatch openMsdf(Renderer2D r, GpuTextureView view, GpuSampler sampler,
                                     float pxRange, int atlasWidth, int atlasHeight) {
        beginScope();
        return finish(r, Renderer2D.UI_BATCHER.getOrCreateMsdf(view, sampler, pxRange, atlasWidth, atlasHeight));
    }

    public static void close() {
        if (depth > 0 && --depth == 0 && rootAuto) {
            rootAuto = false;
            UiRenderDispatcher.endAutoBatch(true);
        }
    }

    private static void beginScope() {
        if (depth == 0) rootAuto = UiRenderDispatcher.beginAutoBatch();
        depth++;
    }

    private static DrawBatch finish(Renderer2D r, DrawBatch batch) {
        if (batch == null) {
            close();
            return null;
        }
        batch.mesh.alpha = r.getAlpha();
        return batch;
    }

    /** Unpacks {@code argb} into the shared scratch as {r, g, b, a}. */
    public static int[] rgba(int argb) {
        RGBA[0] = (argb >>> 16) & 0xFF;
        RGBA[1] = (argb >>> 8) & 0xFF;
        RGBA[2] = argb & 0xFF;
        RGBA[3] = (argb >>> 24) & 0xFF;
        return RGBA;
    }

    /** Unpacks {@code argb} into normalized floats {r, g, b, a} in range 0..1. */
    public static float[] rgbaNorm(int argb) {
        NORM[0] = ((argb >>> 16) & 0xFF) / 255.0f;
        NORM[1] = ((argb >>> 8) & 0xFF) / 255.0f;
        NORM[2] = (argb & 0xFF) / 255.0f;
        NORM[3] = ((argb >>> 24) & 0xFF) / 255.0f;
        return NORM;
    }
}
