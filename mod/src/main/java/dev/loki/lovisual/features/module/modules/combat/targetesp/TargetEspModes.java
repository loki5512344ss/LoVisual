/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.combat.targetesp;

import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.math.RenderMath;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.IntFunction;

import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.*;
import dev.loki.lovisual.features.module.modules.combat.prediction.TargetESP;

/**
 * State-driven TargetESP modes: animation state, the legacy ring, ghost
 * particles and the capture mark.
 */
public final class TargetEspModes {
    private TargetEspModes() {
    }

    private static final Identifier CAPTURE_MARK_TEXTURE = TextureStorage.CAPTURE;
    private static final int VORTEX_HELIX_STEPS = 28;
    private static final int VORTEX_RING_SEGMENTS = 24;

    private static float prevCircleStep;
    private static float circleStep;
    private static double captureValue = 1.0;
    private static double prevCaptureValue;
    private static double captureSpeed = 1.0;
    private static double captureSpeedScale = 1.0;
    private static boolean captureFlipSpeed;

    public static void tickTargetEsp() {
        prevCircleStep = circleStep;
        circleStep += 0.15f;

        prevCaptureValue = captureValue;
        captureValue += captureSpeed * captureSpeedScale;
        if (captureSpeed > 25.0) captureFlipSpeed = true;
        if (captureSpeed < -25.0) captureFlipSpeed = false;
        captureSpeed = captureFlipSpeed
                ? captureSpeed - 0.5 * captureSpeedScale
                : captureSpeed + 0.5 * captureSpeedScale;
    }

    public static void setCaptureSpeedScale(float scale) {
        if (scale <= 0f) {
            captureSpeedScale = 0.05;
        } else {
            captureSpeedScale = scale;
        }
    }

    public static void drawOldTargetEsp(Renderer3D renderer, Entity target, float tickDelta, IntFunction<Integer> palette) {
        if (renderer == null || target == null) return;
        double cs = prevCircleStep + (circleStep - prevCircleStep) * tickDelta;
        double prevSinAnim = absSinAnimation(cs - 0.45f);
        double sinAnim = absSinAnimation(cs);
        Vec3 pos = RenderMath.getLerpedPos(target, tickDelta);
        double x = pos.x;
        double y = pos.y + prevSinAnim * target.getBbHeight();
        double z = pos.z;
        double nextY = pos.y + sinAnim * target.getBbHeight();

        MeshBuilder mesh = renderer.tris();

        final int segments = 96;
        final double step = Math.PI * 2.0 / segments;
        final double radius = target.getBbWidth() * 0.8;

        for (int i = 0; i < segments; i++) {
            double a0 = i * step;
            double a1 = (i + 1) * step;
            float cos = (float) (x + Math.cos(a0) * radius);
            float sin = (float) (z + Math.sin(a0) * radius);
            float cosNext = (float) (x + Math.cos(a1) * radius);
            float sinNext = (float) (z + Math.sin(a1) * radius);

            int idx0 = (int) Math.round(i * (360.0 / segments));
            int idx1 = (int) Math.round((i + 1) * (360.0 / segments));
            int cTop0 = applyOpacity(palette.apply(idx0), 170.0f / 255.0f);
            int cTop1 = applyOpacity(palette.apply(idx1), 170.0f / 255.0f);
            int cBottom0 = applyOpacity(palette.apply(idx0), 0.0f);
            int cBottom1 = applyOpacity(palette.apply(idx1), 0.0f);

            mesh.ensureQuadCapacity();
            int i1 = addVertex(mesh, cos, nextY, sin, cTop0);
            int i2 = addVertex(mesh, cosNext, nextY, sinNext, cTop1);
            int i3 = addVertex(mesh, cosNext, y, sinNext, cBottom1);
            int i4 = addVertex(mesh, cos, y, sin, cBottom0);
            mesh.quad(i1, i2, i3, i4);
        }

    }

    public static void renderVortex(Renderer3D renderer,
                                    LivingEntity target,
                                    float tickDelta,
                                    IntFunction<Integer> palette,
                                    float spinSpeed) {
        if (renderer == null || target == null) return;

        Vec3 pos = RenderMath.getLerpedPos(target, tickDelta);
        float radius = target.getBbWidth() * 0.7f;
        float height = target.getBbHeight();
        double time = (GameClock.millis() / 1000.0) * spinSpeed;

        float prevWidth = RenderState.lineWidth;
        RenderState.lineWidth = 1.4f;
        MeshBuilder mesh;
        try {
            mesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_LINES, Renderer3D.DepthMode.NONE);
        } finally {
            RenderState.lineWidth = prevWidth;
        }
        if (mesh == null) return;

        for (int strand = 0; strand < 2; strand++) {
            double strandOffset = strand * Math.PI;
            int prevIndex = -1;
            for (int i = 0; i <= VORTEX_HELIX_STEPS; i++) {
                double t = (double) i / VORTEX_HELIX_STEPS;
                double angle = time * 2.4 + t * Math.PI * 3.0 + strandOffset;
                double r = radius * (0.85 + 0.15 * Math.sin(t * Math.PI));
                double x = pos.x + Math.cos(angle) * r;
                double y = pos.y + t * height;
                double z = pos.z + Math.sin(angle) * r;
                int color = applyOpacity(palette.apply((int) (t * 180)), (float) (1.0 - t * 0.35));

                int index = addVertex(mesh, x, y, z, color);
                if (prevIndex >= 0) {
                    mesh.line(prevIndex, index);
                }
                prevIndex = index;
            }
        }

        float[] ringHeights = {0.05f, height * 0.5f, height * 0.95f};
        float[] ringSpeeds = {0.4f, -0.55f, 0.35f};
        for (int ring = 0; ring < ringHeights.length; ring++) {
            double ringY = pos.y + ringHeights[ring];
            double ringRotation = time * ringSpeeds[ring];
            int ringColor = applyOpacity(palette.apply(60), 0.4f);
            int prevIndex = -1;
            int firstIndex = -1;
            for (int i = 0; i <= VORTEX_RING_SEGMENTS; i++) {
                double angle = ringRotation + (i % VORTEX_RING_SEGMENTS) * 2.0 * Math.PI / VORTEX_RING_SEGMENTS;
                double x = pos.x + Math.cos(angle) * radius;
                double z = pos.z + Math.sin(angle) * radius;
                int index = addVertex(mesh, x, ringY, z, ringColor);
                if (firstIndex < 0) {
                    firstIndex = index;
                }
                if (prevIndex >= 0) {
                    mesh.line(prevIndex, index);
                }
                prevIndex = index;
            }
        }
    }

    public static void renderCaptureMark(Renderer3D renderer,
                                         Entity target,
                                         float tickDelta,
                                         IntFunction<Integer> palette) {
        if (renderer == null) return;
        Vec3 pos = RenderMath.getLerpedPos(target, tickDelta);
        double tPosX = pos.x;
        double tPosY = pos.y;
        double tPosZ = pos.z;

        MeshBuilder mesh = renderer.batchTextured(
                LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE,
                CAPTURE_MARK_TEXTURE,
                Renderer3D.DepthMode.MAIN
        );
        if (mesh == null) return;

        float angle = interpolateFloat((float) prevCaptureValue, (float) captureValue, tickDelta);
        float size = 0.75f;

        double cy = tPosY + target.getEyeHeight(target.getPose()) / 2f;

        addRotatedBillboard(mesh, tPosX, cy, tPosZ, size, angle, palette);
    }
}
