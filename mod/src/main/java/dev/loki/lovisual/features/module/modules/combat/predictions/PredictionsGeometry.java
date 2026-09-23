/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.combat.predictions;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import dev.loki.lovisual.features.module.modules.combat.prediction.Predictions;

/**
 * Low level mesh primitives, impact shapes and small vector/color math shared by
 * the Predictions renderers.
 */
public final class PredictionsGeometry {
    private PredictionsGeometry() {
    }

    private static final int IMPACT_RING_SEGMENTS = 64;
    private static final float IMPACT_RING_CROSS_RADIUS = 0.18f;
    private static final float IMPACT_DECAL_EDGE_SOFTNESS = 0.085f;
    private static final float IMPACT_DECAL_STROKE_WIDTH = 0.10f;
    private static final float IMPACT_DECAL_PATTERN_SCALE = 4.25f;
    private static final float IMPACT_DECAL_FILL_ALPHA = 0.48f;
    private static final float IMPACT_DECAL_STROKE_ALPHA = 0.92f;
    private static final float IMPACT_DECAL_PATTERN_STRENGTH = 0.10f;
    private static final float IMPACT_DECAL_QUAD_PADDING = 1.18f;
    private static final double IMPACT_DECAL_OFFSET = 0.004;

    public static void addGradientLine(MeshBuilder mesh, Vec3 from, Vec3 to, int startColor, int endColor) {
        if (mesh == null || from == null || to == null) return;
        mesh.ensureLineCapacity();
        int i1 = mesh.vec3(from.x, from.y, from.z).color(new RenderColor(startColor)).next();
        int i2 = mesh.vec3(to.x, to.y, to.z).color(new RenderColor(endColor)).next();
        mesh.line(i1, i2);
    }

    public static void addImpactRing(MeshBuilder mesh, Vec3 center, Vec3 normal, float radius, int argb) {
        if (mesh == null || center == null) return;

        Vec3 useNormal = normal == null || normal.lengthSqr() < 1.0E-6
                ? new Vec3(0.0, 1.0, 0.0)
                : normal.normalize();
        Vec3 axis = Math.abs(useNormal.y) < 0.95 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 tangent = useNormal.cross(axis);
        if (tangent.lengthSqr() < 1.0E-6) {
            tangent = new Vec3(1.0, 0.0, 0.0);
        } else {
            tangent = tangent.normalize();
        }
        Vec3 bitangent = useNormal.cross(tangent).normalize();

        for (int i = 0; i < IMPACT_RING_SEGMENTS; i++) {
            double angle0 = (Math.PI * 2.0 * i) / IMPACT_RING_SEGMENTS;
            double angle1 = (Math.PI * 2.0 * (i + 1)) / IMPACT_RING_SEGMENTS;
            Vec3 p0 = center
                    .add(tangent.scale(Math.cos(angle0) * radius))
                    .add(bitangent.scale(Math.sin(angle0) * radius));
            Vec3 p1 = center
                    .add(tangent.scale(Math.cos(angle1) * radius))
                    .add(bitangent.scale(Math.sin(angle1) * radius));
            addGradientLine(mesh, p0, p1, argb, argb);
        }

        Vec3 crossTangent = tangent.scale(IMPACT_RING_CROSS_RADIUS);
        Vec3 crossBitangent = bitangent.scale(IMPACT_RING_CROSS_RADIUS);
        addGradientLine(mesh, center.subtract(crossTangent), center.add(crossTangent), argb, argb);
        addGradientLine(mesh, center.subtract(crossBitangent), center.add(crossBitangent), argb, argb);
    }

    public static void addImpactDecal(MeshBuilder mesh, Vec3 center, Vec3 normal, float radius, int argb) {
        if (mesh == null || center == null || radius <= 0.0f) {
            return;
        }

        Vec3 useNormal = normal == null || normal.lengthSqr() < 1.0E-6
                ? new Vec3(0.0, 1.0, 0.0)
                : normal.normalize();
        Vec3 axis = Math.abs(useNormal.y) < 0.95 ? new Vec3(0.0, 1.0, 0.0) : new Vec3(1.0, 0.0, 0.0);
        Vec3 tangent = useNormal.cross(axis);
        if (tangent.lengthSqr() < 1.0E-6) {
            tangent = new Vec3(1.0, 0.0, 0.0);
        } else {
            tangent = tangent.normalize();
        }
        Vec3 bitangent = useNormal.cross(tangent).normalize();
        Vec3 offsetCenter = center.add(useNormal.scale(IMPACT_DECAL_OFFSET));
        float quadRadius = radius * IMPACT_DECAL_QUAD_PADDING;
        float sdfRadius = 1.0f / IMPACT_DECAL_QUAD_PADDING;
        Vec3 tangentScaled = tangent.scale(quadRadius);
        Vec3 bitangentScaled = bitangent.scale(quadRadius);

        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(offsetCenter.subtract(tangentScaled).subtract(bitangentScaled).x,
                        offsetCenter.subtract(tangentScaled).subtract(bitangentScaled).y,
                        offsetCenter.subtract(tangentScaled).subtract(bitangentScaled).z)
                .vec2(0.0, 0.0)
                .color(new RenderColor(argb))
                .vec4(sdfRadius, IMPACT_DECAL_EDGE_SOFTNESS, IMPACT_DECAL_STROKE_WIDTH, IMPACT_DECAL_PATTERN_SCALE)
                .vec4(IMPACT_DECAL_FILL_ALPHA, IMPACT_DECAL_STROKE_ALPHA, IMPACT_DECAL_PATTERN_STRENGTH, 0.0f)
                .next();
        int i2 = mesh.vec3(offsetCenter.add(tangentScaled).subtract(bitangentScaled).x,
                        offsetCenter.add(tangentScaled).subtract(bitangentScaled).y,
                        offsetCenter.add(tangentScaled).subtract(bitangentScaled).z)
                .vec2(1.0, 0.0)
                .color(new RenderColor(argb))
                .vec4(sdfRadius, IMPACT_DECAL_EDGE_SOFTNESS, IMPACT_DECAL_STROKE_WIDTH, IMPACT_DECAL_PATTERN_SCALE)
                .vec4(IMPACT_DECAL_FILL_ALPHA, IMPACT_DECAL_STROKE_ALPHA, IMPACT_DECAL_PATTERN_STRENGTH, 0.0f)
                .next();
        int i3 = mesh.vec3(offsetCenter.add(tangentScaled).add(bitangentScaled).x,
                        offsetCenter.add(tangentScaled).add(bitangentScaled).y,
                        offsetCenter.add(tangentScaled).add(bitangentScaled).z)
                .vec2(1.0, 1.0)
                .color(new RenderColor(argb))
                .vec4(sdfRadius, IMPACT_DECAL_EDGE_SOFTNESS, IMPACT_DECAL_STROKE_WIDTH, IMPACT_DECAL_PATTERN_SCALE)
                .vec4(IMPACT_DECAL_FILL_ALPHA, IMPACT_DECAL_STROKE_ALPHA, IMPACT_DECAL_PATTERN_STRENGTH, 0.0f)
                .next();
        int i4 = mesh.vec3(offsetCenter.subtract(tangentScaled).add(bitangentScaled).x,
                        offsetCenter.subtract(tangentScaled).add(bitangentScaled).y,
                        offsetCenter.subtract(tangentScaled).add(bitangentScaled).z)
                .vec2(0.0, 1.0)
                .color(new RenderColor(argb))
                .vec4(sdfRadius, IMPACT_DECAL_EDGE_SOFTNESS, IMPACT_DECAL_STROKE_WIDTH, IMPACT_DECAL_PATTERN_SCALE)
                .vec4(IMPACT_DECAL_FILL_ALPHA, IMPACT_DECAL_STROKE_ALPHA, IMPACT_DECAL_PATTERN_STRENGTH, 0.0f)
                .next();
        mesh.quad(i1, i2, i3, i4);
    }

    public static void addBillboardQuad(MeshBuilder mesh, double cx, double cy, double cz,
                                        float size, Quaternionf camRot, int argb) {
        Vector3f right = new Vector3f(1.0f, 0.0f, 0.0f).rotate(camRot).mul(size);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f).rotate(camRot).mul(size);

        double p1x = cx - right.x() - up.x();
        double p1y = cy - right.y() - up.y();
        double p1z = cz - right.z() - up.z();
        double p2x = cx + right.x() - up.x();
        double p2y = cy + right.y() - up.y();
        double p2z = cz + right.z() - up.z();
        double p3x = cx + right.x() + up.x();
        double p3y = cy + right.y() + up.y();
        double p3z = cz + right.z() + up.z();
        double p4x = cx - right.x() + up.x();
        double p4y = cy - right.y() + up.y();
        double p4z = cz - right.z() + up.z();

        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(p1x, p1y, p1z).vec2(0.0, 1.0).color(new RenderColor(argb)).next();
        int i2 = mesh.vec3(p2x, p2y, p2z).vec2(1.0, 1.0).color(new RenderColor(argb)).next();
        int i3 = mesh.vec3(p3x, p3y, p3z).vec2(1.0, 0.0).color(new RenderColor(argb)).next();
        int i4 = mesh.vec3(p4x, p4y, p4z).vec2(0.0, 0.0).color(new RenderColor(argb)).next();
        mesh.quad(i1, i2, i3, i4);
    }

    public static int multiplyAlpha(int argb, float alphaMultiplier) {
        int baseAlpha = (argb >>> 24) & 0xFF;
        int alpha = Math.max(0, Math.min(255, Math.round(baseAlpha * Math.max(0.0f, alphaMultiplier))));
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    public static double partialStepProgress(Vec3 from, Vec3 hitPos, double segmentLengthSq) {
        if (from == null || hitPos == null || segmentLengthSq <= 1.0E-6) {
            return 1.0;
        }
        return Mth.clamp(from.distanceTo(hitPos) / Math.sqrt(segmentLengthSq), 0.0, 1.0);
    }

    public static Vec3 resolveImpactNormal(HitResult hit, Vec3 velocity) {
        if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit) {
            return Vec3.atLowerCornerOf(blockHit.getDirection().getUnitVec3i());
        }
        if (velocity != null && velocity.lengthSqr() > 1.0E-6) {
            return velocity.normalize();
        }
        return new Vec3(0.0, 1.0, 0.0);
    }

    public static Vec3 advanceTickVelocity(Entity entity, Vec3 velocity) {
        return velocity.scale(0.99).add(0.0, -entity.getGravity(), 0.0);
    }
}
