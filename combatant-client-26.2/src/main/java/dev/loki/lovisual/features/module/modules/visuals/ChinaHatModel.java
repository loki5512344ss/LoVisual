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
 * Geometry for the classic ChinaHat: a wide flat brim (annulus) plus a conical dome.
 * Pure mesh assembly, no Minecraft state. Per-vertex colors come from the caller.
 */
final class ChinaHatModel {
    private ChinaHatModel() {
    }

    interface Shade {
        int color(int index, float verticalLight, float alphaMul);
    }

    static void build(MeshBuilder tris,
                      MeshBuilder lines,
                      HeadAnchor anchor,
                      int segments,
                      float brimRadius,
                      float domeRadius,
                      float domeHeight,
                      float brimThickness,
                      float yawDeg,
                      float pitchDeg,
                      float rollDeg,
                      Shade domeShade,
                      Shade brimShade,
                      Shade rimShade,
                      float alphaMul) {
        if (tris == null || lines == null || segments < 3) return;

        float outerY = brimThickness;
        float innerY = 0.0f;
        Vec3[] outerTop = new Vec3[segments];
        Vec3[] outerBottom = new Vec3[segments];
        Vec3[] innerTop = new Vec3[segments];
        Vec3[] innerBottom = new Vec3[segments];

        for (int i = 0; i < segments; i++) {
            double angle = (Math.PI * 2.0 * i) / segments;
            float s = Mth.sin((float) angle);
            float c = Mth.cos((float) angle);
            outerTop[i] = anchor.localRotated(s * brimRadius, outerY, c * brimRadius, yawDeg, pitchDeg, rollDeg);
            outerBottom[i] = anchor.localRotated(s * brimRadius, innerY, c * brimRadius, yawDeg, pitchDeg, rollDeg);
            innerTop[i] = anchor.localRotated(s * domeRadius, outerY, c * domeRadius, yawDeg, pitchDeg, rollDeg);
            innerBottom[i] = anchor.localRotated(s * domeRadius, innerY, c * domeRadius, yawDeg, pitchDeg, rollDeg);
        }

        Vec3 tip = anchor.localRotated(0.0f, outerY + domeHeight, 0.0f, yawDeg, pitchDeg, rollDeg);

        for (int i = 0; i < segments; i++) {
            int next = (i + 1) % segments;

            int domeA = domeShade.color(i, 1.06f, alphaMul);
            int domeB = domeShade.color(next, 1.02f, alphaMul);
            triangle(tris, tip, innerTop[i], domeA, innerTop[next], domeB);

            int brimTopA = brimShade.color(i, 0.94f, alphaMul);
            int brimTopB = brimShade.color(next, 0.90f, alphaMul);
            quad(tris,
                    innerTop[i], brimTopA,
                    innerTop[next], brimTopB,
                    outerTop[next], brimTopB,
                    outerTop[i], brimTopA);

            int brimBotA = brimShade.color(i, 0.42f, alphaMul);
            int brimBotB = brimShade.color(next, 0.40f, alphaMul);
            quad(tris,
                    outerBottom[i], brimBotA,
                    outerBottom[next], brimBotB,
                    innerBottom[next], brimBotA,
                    innerBottom[i], brimBotB);

            int edgeA = brimShade.color(i, 0.72f, alphaMul);
            int edgeB = brimShade.color(next, 0.70f, alphaMul);
            quad(tris,
                    outerBottom[i], edgeA,
                    outerBottom[next], edgeB,
                    outerTop[next], edgeB,
                    outerTop[i], edgeA);

            int rim1 = rimShade.color(i, 1.0f, alphaMul);
            int rim2 = rimShade.color(next, 1.0f, alphaMul);
            lineGradient(lines, outerTop[i], outerTop[next], rim1, rim2);
        }
    }

    private static void triangle(MeshBuilder mesh, Vec3 a, Vec3 b, int cA, Vec3 c, int cC) {
        mesh.ensureTriCapacity();
        int i1 = putVertex(mesh, a, cA);
        int i2 = putVertex(mesh, b, cA);
        int i3 = putVertex(mesh, c, cC);
        mesh.triangle(i1, i2, i3);
    }

    private static void quad(MeshBuilder mesh,
                             Vec3 p1, int c1,
                             Vec3 p2, int c2,
                             Vec3 p3, int c3,
                             Vec3 p4, int c4) {
        mesh.ensureQuadCapacity();
        int i1 = putVertex(mesh, p1, c1);
        int i2 = putVertex(mesh, p2, c2);
        int i3 = putVertex(mesh, p3, c3);
        int i4 = putVertex(mesh, p4, c4);
        mesh.quad(i1, i2, i3, i4);
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
