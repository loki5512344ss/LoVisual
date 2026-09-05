/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.combat.targetesp;

import dev.loki.lovisual.render.engine.TextureStorage;
import dev.loki.lovisual.render.engine.math.RenderMath;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.IntFunction;

import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.*;

public final class TargetEspEffects {
    private TargetEspEffects() {
    }

    private static final Identifier GHOST_PARTICLE_TEXTURE = TextureStorage.FIRE_FLY;

    private static final float[][] BODY_HOLES = {
            {0.0f, 0.60f},   // голова
            {-0.48f, 0.36f}, // левая рука
            {0.48f, 0.36f},  // правая рука
            {0.0f, 0.05f},   // грудь
            {-0.30f, -0.42f},// левая нога
            {0.30f, -0.42f}  // правая нога
    };

    public static void renderGhosts(Renderer3D renderer,
                                    int espLength, int factor, float shaking, float amplitude,
                                    Entity target, float tickDelta, IntFunction<Integer> palette) {
        Minecraft mc = Minecraft.getInstance();
        if (renderer == null || mc == null || mc.player == null) return;

        Vec3 pos = RenderMath.getLerpedPos(target, tickDelta);
        double tPosX = pos.x;
        double tPosY = pos.y;
        double tPosZ = pos.z;
        float iAge = (float) interpolate(target.tickCount - 1, target.tickCount, tickDelta);

        MeshBuilder mesh = renderer.batchTextured(
                LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE,
                GHOST_PARTICLE_TEXTURE,
                Renderer3D.DepthMode.MAIN
        );
        if (mesh == null) return;

        for (int j = 0; j < 3; j++) {
            for (int i = 0; i <= espLength; i++) {
                double radians = Math.toRadians((((float) i / 1.5f + iAge) * factor + (j * 120)) % (factor * 360));
                double sinQuad = Math.sin(Math.toRadians(iAge * 2.5f + i * (j + 1)) * amplitude) / shaking;

                float offset = ((float) i / espLength);
                int color = applyOpacity(palette.apply((int) (180 * offset)), offset);
                float scale = Math.max(0.24f * (offset), 0.2f);

                double cx = tPosX + Math.cos(radians) * target.getBbWidth();
                double cy = tPosY + 1 + sinQuad;
                double cz = tPosZ + Math.sin(radians) * target.getBbWidth();

                addBillboardQuad(mesh, cx, cy, cz, scale, color);
            }
        }
    }

    public static void renderBlackhole(Renderer3D renderer,
                                       LivingEntity target,
                                       float tickDelta,
                                       IntFunction<Integer> palette,
                                       float size,
                                       float spinSpeed,
                                       float alpha,
                                       String style) {
        if (renderer == null || target == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameRenderer == null || mc.gameRenderer.mainCamera() == null) return;
        Vec3 pos = RenderMath.getLerpedPos(target, tickDelta);
        Vec3 baseCenter = new Vec3(pos.x, pos.y + target.getBbHeight() * 0.5, pos.z);

        // Камера-зависимый billboard-базис: правый/верх в мировых координатах
        Camera cam = mc.gameRenderer.mainCamera();
        float yaw = (float) Math.toRadians(cam.yRot());
        float pitch = (float) Math.toRadians(cam.xRot());
        Vec3 fwd = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch)).normalize();
        Vec3 rgt = new Vec3(0.0, 1.0, 0.0).cross(fwd);
        if (rgt.lengthSqr() < 1.0E-6) {
            rgt = new Vec3(1.0, 0.0, 0.0);
        } else {
            rgt = rgt.normalize();
        }
        Vec3 upp = fwd.cross(rgt).normalize();
        Vector3f right = new Vector3f((float) rgt.x, (float) rgt.y, (float) rgt.z);
        Vector3f up = new Vector3f((float) upp.x, (float) upp.y, (float) upp.z);

        MeshBuilder diskMesh = renderer.batch(
                LoVisualRenderPipelines.WORLD_COLORED_ADDITIVE_DEPTH,
                Renderer3D.DepthMode.PRE_DEPTH
        );
        MeshBuilder coreMesh = renderer.batch(
                LoVisualRenderPipelines.WORLD_COLORED_DEPTH,
                Renderer3D.DepthMode.PRE_DEPTH
        );
        if (diskMesh == null || coreMesh == null) return;

        float time = (GameClock.millis() % 100000L) / 1000.0f;

        if ("Body".equals(style)) {
            float holeSize = size * 0.34f;
            for (float[] off : BODY_HOLES) {
                Vec3 center = new Vec3(
                        baseCenter.x + rgt.x * off[0] + upp.x * off[1],
                        baseCenter.y + rgt.y * off[0] + upp.y * off[1],
                        baseCenter.z + rgt.z * off[0] + upp.z * off[1]
                );
                appendBlackhole(diskMesh, coreMesh, center, right, up, holeSize, time, spinSpeed, palette, alpha);
            }
        } else {
            appendBlackhole(diskMesh, coreMesh, baseCenter, right, up, size, time, spinSpeed, palette, alpha);
        }
    }

    private static void appendBlackhole(MeshBuilder diskMesh,
                                        MeshBuilder coreMesh,
                                        Vec3 center,
                                        Vector3f right,
                                        Vector3f up,
                                        float size,
                                        float time,
                                        float spinSpeed,
                                        IntFunction<Integer> palette,
                                        float alpha) {
        float horizon = 0.35f * size;
        int rings = 4;
        int segs = 64;

        // аккреционный диск: вращающаяся спираль на аддитивном бленде
        for (int ring = 0; ring < rings; ring++) {
            float r0 = horizon + (size - horizon) * (ring / (float) rings);
            float r1 = horizon + (size - horizon) * ((ring + 1) / (float) rings);
            for (int s = 0; s < segs; s++) {
                float a0 = (float) (Math.PI * 2.0 * s / segs);
                float a1 = (float) (Math.PI * 2.0 * (s + 1) / segs);
                int v0 = addBlackholeVertex(diskMesh, center, right, up, r0, a0, horizon, size, time, spinSpeed, palette);
                int v1 = addBlackholeVertex(diskMesh, center, right, up, r1, a0, horizon, size, time, spinSpeed, palette);
                int v2 = addBlackholeVertex(diskMesh, center, right, up, r1, a1, horizon, size, time, spinSpeed, palette);
                int v3 = addBlackholeVertex(diskMesh, center, right, up, r0, a1, horizon, size, time, spinSpeed, palette);
                diskMesh.triangle(v0, v1, v2);
                diskMesh.triangle(v0, v2, v3);
            }
        }

        // чёрное ядро закрывает центр диска
        int coreArgb = applyOpacity(0xFF080808, alpha);
        int coreCenter = addBlackholeCoreVertex(coreMesh, center, right, up, 0f, 0f, coreArgb);
        int coreSegs = 32;
        for (int s = 0; s < coreSegs; s++) {
            float a0 = (float) (Math.PI * 2.0 * s / coreSegs);
            float a1 = (float) (Math.PI * 2.0 * (s + 1) / coreSegs);
            int c0 = addBlackholeCoreVertex(coreMesh, center, right, up, horizon, a0, coreArgb);
            int c1 = addBlackholeCoreVertex(coreMesh, center, right, up, horizon, a1, coreArgb);
            coreMesh.triangle(coreCenter, c1, c0);
        }
    }
}
