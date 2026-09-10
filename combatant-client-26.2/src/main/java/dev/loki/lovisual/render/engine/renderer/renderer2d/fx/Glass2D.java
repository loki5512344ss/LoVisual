/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.renderer2d.fx;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Batch2D;
import dev.loki.lovisual.render.engine.renderer.ui.blur.BlurSource;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchType;
import dev.loki.lovisual.render.engine.renderer.ui.draw.effect.UiEffectSpec;
import dev.loki.lovisual.render.engine.renderer.ui.draw.shape.UiShape;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.clamp01;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.normalizeCornerRadii;
import static dev.loki.lovisual.render.engine.renderer.ui.draw.edge.UiMeshGeometry.packLiquidGlassPayload;

/** Liquid glass material rectangles backed by the frame blur pipeline. */
public final class Glass2D {
    private Glass2D() {
    }

    // squirclePower < 1.5f disables the squircle falloff
    public static void liquidGlassRectCorners(Renderer2D r, double x, double y, double w, double h,
                                              float radiusTL, float radiusTR, float radiusBR, float radiusBL,
                                              float softness,
                                              int tintArgb,
                                              float glassAlpha,
                                              float blurAlpha,
                                              float fresnelPower,
                                              float fresnelAlpha,
                                              float baseAlpha,
                                              float fresnelMix,
                                              float distortPx,
                                              float squirclePower) {
        if (w <= 0.0 || h <= 0.0) return;

        r.effect(UiEffectSpec.liquidGlass(UiShape.roundedRect(x, y, w, h, radiusTL, radiusTR, radiusBR, radiusBL),
                softness, softness, distortPx, tintArgb));
        BlurSource source = Blur2D.source();
        if (source == null) return;

        DrawBatch batch = Batch2D.openBlur(r, UiBatchType.LIQUID_GLASS, source.view, source.sampler,
                Renderer2D.DEFAULT_LIQUID_GLASS_BLUR_QUALITY, Renderer2D.LIQUID_GLASS_KAWASE_OFFSET_PX);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int[] tint = Batch2D.rgba(tintArgb);
        float clampedBlurAlpha = clamp01(blurAlpha);
        int finalA = (int) (tint[3] * clamp01(glassAlpha));

        float[] radii = Batch2D.CORNER_RADII;
        normalizeCornerRadii(w, h, radiusTL, radiusTR, radiusBR, radiusBL, radii);

        float thickness = Math.max(0.0f, softness);
        float mix = clamp01(fresnelMix);
        float fa = clamp01(fresnelAlpha);
        float ba = clamp01(baseAlpha);
        float packedDistort = packLiquidGlassPayload(distortPx, squirclePower, clampedBlurAlpha);

        int i1 = glassVertex(mesh, x, y, x, y, w, h, tint, finalA, radii, mix, packedDistort, thickness, fresnelPower, fa, ba);
        int i2 = glassVertex(mesh, x, y + h, x, y, w, h, tint, finalA, radii, mix, packedDistort, thickness, fresnelPower, fa, ba);
        int i3 = glassVertex(mesh, x + w, y + h, x, y, w, h, tint, finalA, radii, mix, packedDistort, thickness, fresnelPower, fa, ba);
        int i4 = glassVertex(mesh, x + w, y, x, y, w, h, tint, finalA, radii, mix, packedDistort, thickness, fresnelPower, fa, ba);
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    /**
     * One liquid-glass vertex. {@code px}/{@code py} is the vertex position;
     * {@code rx}/{@code ry} is the rect origin carried by the Rect payload.
     */
    private static int glassVertex(MeshBuilder mesh, double px, double py, double rx, double ry, double w, double h,
                                   int[] tint, int finalA, float[] radii,
                                   float mix, float packedDistort,
                                   float thickness, float fresnelPower, float fresnelAlpha, float baseAlpha) {
        return mesh.vec2(px, py).raw2(mix, packedDistort).local2(px, py).color(tint[0], tint[1], tint[2], finalA)
                .vec4((float) rx, (float) ry, (float) w, (float) h)
                .vec4(radii[0], radii[1], radii[2], radii[3])
                .vec4(thickness, fresnelPower, fresnelAlpha, baseAlpha)
                .next();
    }
}
