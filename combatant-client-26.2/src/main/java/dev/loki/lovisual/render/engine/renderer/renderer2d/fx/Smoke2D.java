/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.renderer2d.fx;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.renderer2d.Batch2D;
import dev.loki.lovisual.render.engine.renderer.ui.batches.DrawBatch;
import dev.loki.lovisual.render.engine.renderer.ui.batches.ordered.UiBatchType;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.util.Mth;

/** Procedural smoke fill for rounded rects (health/progress style bars). */
public final class Smoke2D {
    private Smoke2D() {
    }

    public static void roundedSmokeFill(Renderer2D r, double x, double y, double w, double h,
                                        float radius, float softness,
                                        float fillRatio,
                                        boolean fromRight,
                                        int firstArgb,
                                        int secondArgb,
                                        int thirdArgb,
                                        float time,
                                        float smokeScale,
                                        float smokeMix,
                                        int octaves,
                                        float flowX,
                                        float flowY,
                                        float intensity) {
        if (w <= 0.0 || h <= 0.0) return;
        float fill = Mth.clamp(fillRatio, 0.0f, 1.0f);
        if (fill <= 0.0001f) return;

        DrawBatch batch = Batch2D.open(r, UiBatchType.ROUNDED_FILL_SMOKE);
        if (batch == null) return;
        MeshBuilder mesh = batch.mesh;
        mesh.ensureQuadCapacity();

        int[] first = Batch2D.rgba(firstArgb);
        float[] second = Batch2D.rgbaNorm(secondArgb);
        float[] third = Batch2D.rgbaNorm(thirdArgb);

        float fromRightFlag = fromRight ? 1.0f : 0.0f;
        float safeScale = Math.max(0.1f, smokeScale);
        float safeMix = Mth.clamp(smokeMix, 0.0f, 1.0f);
        float safeOctaves = Mth.clamp(octaves, 1, 8);
        float safeIntensity = Math.max(0.0f, intensity);

        int i1 = smokeVertex(mesh, x, y, x, y, w, h, first, second, third, radius, softness, fill, fromRightFlag,
                time, safeScale, safeMix, safeOctaves, flowX, flowY, safeIntensity);
        int i2 = smokeVertex(mesh, x, y + h, x, y, w, h, first, second, third, radius, softness, fill, fromRightFlag,
                time, safeScale, safeMix, safeOctaves, flowX, flowY, safeIntensity);
        int i3 = smokeVertex(mesh, x + w, y + h, x, y, w, h, first, second, third, radius, softness, fill, fromRightFlag,
                time, safeScale, safeMix, safeOctaves, flowX, flowY, safeIntensity);
        int i4 = smokeVertex(mesh, x + w, y, x, y, w, h, first, second, third, radius, softness, fill, fromRightFlag,
                time, safeScale, safeMix, safeOctaves, flowX, flowY, safeIntensity);
        mesh.quad(i1, i2, i3, i4);
        Batch2D.close();
    }

    /**
     * One smoke-fill vertex. {@code px}/{@code py} is the vertex position;
     * {@code rx}/{@code ry} is the rect origin carried by the Rect payload
     * (identical for all four vertices of the quad).
     */
    private static int smokeVertex(MeshBuilder mesh, double px, double py, double rx, double ry, double w, double h,
                                   int[] first, float[] second, float[] third,
                                   float radius, float softness, float fill, float fromRightFlag,
                                   float time, float scale, float mix, float octaves,
                                   float flowX, float flowY, float intensity) {
        return mesh.vec2(px, py).local2(px, py).color(first[0], first[1], first[2], first[3])
                .vec4((float) rx, (float) ry, (float) w, (float) h)
                .vec4(radius, softness, fill, fromRightFlag)
                .vec4(second[0], second[1], second[2], second[3])
                .vec4(third[0], third[1], third[2], third[3])
                .vec4(time, scale, mix, octaves)
                .vec4(flowX, flowY, intensity, 0.0f)
                .next();
    }
}
