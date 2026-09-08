/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.helpers.HeadAnchor;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Classic coolie/rice hat: a single smooth cone from the apex resting on the
 * head up to a raised rim circle, like the SoupVisuals reference profile
 * (radius 0.65 vs height 0.3). Rendered double-sided via opposite windings.
 * {@link #layout} is pure geometry (unit-testable); {@link #build} feeds the
 * result into MeshBuilder. Per-vertex colors come from the caller.
 */
final class ChinaHatModel {

    private ChinaHatModel() {
    }

    interface Shade {
        int color(int index, float verticalLight, float alphaMul);
    }

    /** Apex plus rim circle vertices in anchor space; rim[i+1 wraps to rim[0]. */
    record Layout(Vec3 apex, Vec3[] rim) {
    }

    static Layout layout(float brimRadius,
                         float coneHeight,
                         int segments,
                         float yawDeg,
                         float pitchDeg,
                         float rollDeg,
                         HeadAnchor anchor) {
        Vec3 apex = anchor.localRotated(0.0f, coneHeight, 0.0f, yawDeg, pitchDeg, rollDeg);
        Vec3[] rim = new Vec3[Math.max(0, segments)];
        for (int i = 0; i < rim.length; i++) {
            double angle = (Math.PI * 2.0 * i) / segments;
            float s = Mth.sin((float) angle);
            float c = Mth.cos((float) angle);
            rim[i] = anchor.localRotated(s * brimRadius, 0.0f, c * brimRadius,
                    yawDeg, pitchDeg, rollDeg);
        }
        return new Layout(apex, rim);
    }

    static void build(MeshBuilder tris,
                      MeshBuilder lines,
                      HeadAnchor anchor,
                      int segments,
                      float brimRadius,
                      float coneHeight,
                      float yawDeg,
                      float pitchDeg,
                      float rollDeg,
                      Shade coneShade,
                      Shade rimShade,
                      float alphaMul) {
        if (tris == null || segments < 3) return;

        Layout layout = layout(brimRadius, coneHeight, segments, yawDeg, pitchDeg, rollDeg, anchor);
        Vec3 apex = layout.apex();
        Vec3[] rim = layout.rim();

        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;

            int upperA = coneShade.color(i, 1.06f, alphaMul);
            int upperB = coneShade.color(next, 1.02f, alphaMul);
            triangle(tris, apex, rim[i], upperA, rim[next], upperB);
            triangle(tris, apex, rim[next], upperA, rim[i], upperB);

            if (lines == null) continue;
            int rimA = rimShade.color(i, 1.0f, alphaMul);
            int rimB = rimShade.color(next, 1.0f, alphaMul);
            lineGradient(lines, rim[i], rim[next], rimA, rimB);
        }
    }

    private static void triangle(MeshBuilder mesh, Vec3 a, Vec3 b, int cA, Vec3 c, int cC) {
        mesh.ensureTriCapacity();
        int i1 = putVertex(mesh, a, cA);
        int i2 = putVertex(mesh, b, cA);
        int i3 = putVertex(mesh, c, cC);
        mesh.triangle(i1, i2, i3);
    }

    private static void lineGradient(MeshBuilder mesh, Vec3 a, Vec3 b, int cA, int cB) {
        mesh.ensureLineCapacity();
        int i1 = putVertex(mesh, a, cA);
        int i2 = putVertex(mesh, b, cB);
        mesh.line(i1, i2);
    }

    private static int putVertex(MeshBuilder mesh, Vec3 p, int argb) {
        return mesh.vec3(p.x, p.y, p.z)
                .color((argb >>> 24) & 255, (argb >>> 16) & 255, (argb >>> 8) & 255, argb & 255)
                .next();
    }
}
