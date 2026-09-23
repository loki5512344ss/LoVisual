package dev.loki.lovisual.features.module.modules.combat.prediction;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.AimPrediction;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.TrajectoryResult;
import dev.loki.lovisual.features.module.modules.visuals.optimization.FpsOptimizer;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;

import static dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsGeometry.*;
import static dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsSimulation.HIT_ENTITY_COLOR;

public final class PredictionsWorldRenderer {
    private PredictionsWorldRenderer() {}

    private static final int GLOW_SUBSTEPS = 10;
    private static final float GLOW_CORE_SIZE = 0.085f;
    private static final float GLOW_SOFT_SIZE = 0.16f;
    private static final float GLOW_CORE_ALPHA = 0.74f;
    private static final float GLOW_SOFT_ALPHA = 0.32f;
    private static final int AIM_GLOW_SUBSTEPS = 7;
    private static final float AIM_GLOW_CORE_SIZE = 0.055f;
    private static final float AIM_GLOW_SOFT_SIZE = 0.105f;
    private static final float AIM_GLOW_CORE_ALPHA = 0.42f;
    private static final float AIM_GLOW_SOFT_ALPHA = 0.17f;
    private static final double AIM_GLOW_MIN_CAMERA_DISTANCE_SQ = 0.56;
    private static final float IMPACT_RING_RADIUS = 0.30f;
    private static final float IMPACT_RING_LINE_WIDTH = 1.2f;
    private static final float IMPACT_RING_ALPHA = 0.88f;
    private static final float IMPACT_DECAL_RADIUS = 0.34f;
    private static final int ENTITY_HIT_FILL_ALPHA = 34;
    private static final int ENTITY_HIT_LINE_ALPHA = 170;
    private static final float TRAJECTORY_LINE_WIDTH = 1.35f;
    private static final float TRAJECTORY_LINE_ALPHA_START = 0.16f;
    private static final float TRAJECTORY_LINE_ALPHA_END = 0.78f;
    private static final double IMPACT_BOX_EXPAND = 0.03;

    public static void renderAimTrajectoryGlow(Predictions facade, Renderer3D renderer, float tickDelta, int baseColor) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        AimPrediction pred = PredictionsProjectileLogic.computeAimPrediction(facade, player, tickDelta);
        if (pred == null || pred.trajectory() == null) return;
        List<Vec3> points = pred.trajectory().points();
        if (points.size() < 2) return;
        boolean walls = facade.getGlowWallsValue().get();
        RenderPipeline pipeline = walls ? LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE : LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE_LIQUID_IGNORE;
        Renderer3D.DepthMode depthMode = walls ? Renderer3D.DepthMode.MAIN : Renderer3D.DepthMode.PRE_DEPTH;
        if (FpsOptimizer.bloomDisabled()) return;
        MeshBuilder mesh = renderer.batchTextured(pipeline, TextureStorage.BLOOM, depthMode);
        if (mesh == null) return;
        int color = pred.entity() != null ? HIT_ENTITY_COLOR : pred.trajectory().argb() != 0 ? pred.trajectory().argb() : baseColor;
        Quaternionf camRot = RenderState.cameraRotation;
        Vec3 cameraPos = RenderState.cameraPos != null ? RenderState.cameraPos : player.getEyePosition(tickDelta);
        int totalSegments = points.size() - 1;
        for (int i = 1; i < points.size(); i++) {
            Vec3 from = points.get(i - 1);
            Vec3 to = points.get(i);
            for (int step = 0; step <= AIM_GLOW_SUBSTEPS; step++) {
                float stepT = step / (float) AIM_GLOW_SUBSTEPS;
                Vec3 pos = from.lerp(to, stepT);
                if (pos.distanceToSqr(cameraPos) < AIM_GLOW_MIN_CAMERA_DISTANCE_SQ) continue;
                float segmentProgress = ((i - 1) + stepT) / Math.max(1.0f, totalSegments);
                float strength = 0.28f + 0.72f * AnimationUtility.easeOutCubic(segmentProgress);
                addBillboardQuad(mesh, pos.x, pos.y, pos.z, AIM_GLOW_CORE_SIZE, camRot, multiplyAlpha(color, AIM_GLOW_CORE_ALPHA * strength));
                addBillboardQuad(mesh, pos.x, pos.y, pos.z, AIM_GLOW_SOFT_SIZE, camRot, multiplyAlpha(color, AIM_GLOW_SOFT_ALPHA * strength));
            }
        }
    }

    public static void renderAimIndicatorWorld(Predictions facade, Renderer3D renderer, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return;
        AimPrediction pred = PredictionsProjectileLogic.computeAimPrediction(facade, player, tickDelta);
        if (pred == null) return;
        float prevWidth = RenderState.lineWidth;
        RenderState.lineWidth = IMPACT_RING_LINE_WIDTH;
        try {
            MeshBuilder mesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_LINES_LIQUID_IGNORE, Renderer3D.DepthMode.NONE);
            if (mesh == null) return;
            int color = pred.entity() != null ? HIT_ENTITY_COLOR : facade.getLineColorValue().getArgb();
            float pulseTime = (mc.level.getGameTime() + tickDelta) * 0.085f;
            float pulse = 0.9f + 0.1f * AnimationUtility.smoothstep((float) ((Math.sin(pulseTime) + 1.0) * 0.5));
            Vec3 normal = pred.hitNormal() != null ? pred.hitNormal() : new Vec3(0.0, 1.0, 0.0);
            addImpactRing(mesh, pred.hitPos(), normal, IMPACT_RING_RADIUS * pulse, multiplyAlpha(color, IMPACT_RING_ALPHA));
        } finally {
            RenderState.lineWidth = prevWidth;
        }
    }

    public static void renderLineTrajectories(Predictions facade, Renderer3D renderer, List<TrajectoryResult> results, int baseColor) {
        boolean walls = facade.getGlowWallsValue().get();
        RenderPipeline pipeline = walls ? LoVisualRenderPipelines.WORLD_COLORED_LINES_LIQUID_BLEND : LoVisualRenderPipelines.WORLD_COLORED_LINES_LIQUID_IGNORE;
        Renderer3D.DepthMode depthMode = walls ? Renderer3D.DepthMode.MAIN : Renderer3D.DepthMode.PRE_DEPTH;
        float prevWidth = RenderState.lineWidth;
        RenderState.lineWidth = TRAJECTORY_LINE_WIDTH;
        try {
            MeshBuilder mesh = renderer.batch(pipeline, depthMode);
            if (mesh == null) return;
            for (TrajectoryResult res : results) {
                List<Vec3> points = res.points();
                if (points.size() < 2) continue;
                int color = res.argb() != 0 ? res.argb() : baseColor;
                int totalSegments = points.size() - 1;
                for (int i = 1; i < points.size(); i++) {
                    Vec3 from = points.get(i - 1);
                    Vec3 to = points.get(i);
                    float startT = (i - 1) / (float) totalSegments;
                    float endT = i / (float) totalSegments;
                    float startAlpha = Mth.lerp(AnimationUtility.easeOutCubic(startT), TRAJECTORY_LINE_ALPHA_START, TRAJECTORY_LINE_ALPHA_END);
                    float endAlpha = Mth.lerp(AnimationUtility.easeOutCubic(endT), TRAJECTORY_LINE_ALPHA_START, TRAJECTORY_LINE_ALPHA_END);
                    addGradientLine(mesh, from, to, multiplyAlpha(color, startAlpha), multiplyAlpha(color, endAlpha));
                }
            }
        } finally {
            RenderState.lineWidth = prevWidth;
        }
    }

    public static void renderImpactEffects(Predictions facade, Renderer3D renderer, List<TrajectoryResult> results, int baseColor, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        boolean walls = facade.getGlowWallsValue().get();
        RenderPipeline linePipeline = walls ? LoVisualRenderPipelines.WORLD_COLORED_LINES_LIQUID_BLEND : LoVisualRenderPipelines.WORLD_COLORED_LINES_LIQUID_IGNORE;
        Renderer3D.DepthMode lineDepthMode = walls ? Renderer3D.DepthMode.MAIN : Renderer3D.DepthMode.PRE_DEPTH;
        RenderPipeline decalPipeline = walls ? LoVisualRenderPipelines.WORLD_DECAL_SDF : LoVisualRenderPipelines.WORLD_DECAL_SDF_DEPTH;
        Renderer3D.DepthMode decalDepthMode = walls ? Renderer3D.DepthMode.NONE : Renderer3D.DepthMode.PRE_DEPTH;
        float prevWidth = RenderState.lineWidth;
        RenderState.lineWidth = IMPACT_RING_LINE_WIDTH;
        try {
            MeshBuilder decalMesh = renderer.batch(decalPipeline, decalDepthMode);
            renderer.batch(linePipeline, lineDepthMode);
            for (TrajectoryResult res : results) {
                Vec3 impact = res.impactPos();
                if (impact != null && decalMesh != null) {
                    int color = res.argb() != 0 ? res.argb() : baseColor;
                    float pulseTime = (mc.level.getGameTime() + tickDelta) * 0.085f;
                    float pulse = 0.85f + 0.15f * AnimationUtility.smoothstep((float) ((Math.sin(pulseTime) + 1.0) * 0.5));
                    Vec3 normal = res.impactNormal() != null ? res.impactNormal() : new Vec3(0.0, 1.0, 0.0);
                    addImpactDecal(decalMesh, impact, normal, IMPACT_DECAL_RADIUS * pulse, color);
                }
                if (res.hitEntity() != null && res.hitEntity().isAlive()) {
                    int color = res.argb() != 0 ? res.argb() : baseColor;
                    AABB box = res.hitEntity().getBoundingBox().inflate(IMPACT_BOX_EXPAND);
                    addFilledBox(renderer, box, withAlpha(color, ENTITY_HIT_FILL_ALPHA));
                    addOutlineBox(renderer, box, withAlpha(color, ENTITY_HIT_LINE_ALPHA));
                }
            }
        } finally {
            RenderState.lineWidth = prevWidth;
        }
    }

    private static void addFilledBox(Renderer3D renderer, AABB box, int argb) {
        int a = (argb >>> 24) & 0xFF; int r = (argb >>> 16) & 0xFF; int g = (argb >>> 8) & 0xFF; int b = argb & 0xFF;
        double minX = box.minX, minY = box.minY, minZ = box.minZ, maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;
        renderer.quad(minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        renderer.quad(minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, r, g, b, a);
        renderer.quad(minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        renderer.quad(minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, r, g, b, a);
        renderer.quad(maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, r, g, b, a);
        renderer.quad(minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
    }

    private static void addOutlineBox(Renderer3D renderer, AABB box, int argb) {
        int a = (argb >>> 24) & 0xFF; int r = (argb >>> 16) & 0xFF; int g = (argb >>> 8) & 0xFF; int b = argb & 0xFF;
        double minX = box.minX, minY = box.minY, minZ = box.minZ, maxX = box.maxX, maxY = box.maxY, maxZ = box.maxZ;
        renderer.line(minX, minY, minZ, maxX, minY, minZ, r, g, b, a);
        renderer.line(maxX, minY, minZ, maxX, minY, maxZ, r, g, b, a);
        renderer.line(maxX, minY, maxZ, minX, minY, maxZ, r, g, b, a);
        renderer.line(minX, minY, maxZ, minX, minY, minZ, r, g, b, a);
        renderer.line(minX, maxY, minZ, maxX, maxY, minZ, r, g, b, a);
        renderer.line(maxX, maxY, minZ, maxX, maxY, maxZ, r, g, b, a);
        renderer.line(maxX, maxY, maxZ, minX, maxY, maxZ, r, g, b, a);
        renderer.line(minX, maxY, maxZ, minX, maxY, minZ, r, g, b, a);
        renderer.line(minX, minY, minZ, minX, maxY, minZ, r, g, b, a);
        renderer.line(maxX, minY, minZ, maxX, maxY, minZ, r, g, b, a);
        renderer.line(maxX, minY, maxZ, maxX, maxY, maxZ, r, g, b, a);
        renderer.line(minX, minY, maxZ, minX, maxY, maxZ, r, g, b, a);
    }
}
