package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import dev.loki.lovisual.mixininterface.entity.IEntity;
import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.helpers.player.TrailPoint;

final class TrailsRenderer {
    private static final int CONNECT_MAX_STEPS = 6;

    void renderTail(Renderer3D renderer, float tickDelta, boolean useDepth,
                    float scale, boolean connected, float step, int phaseBase,
                    TrailsColor colors, TrailsGeometry geo,
                    String colorMode, int colorSpeed, int primary, int secondary,
                    int lengthSpread, int ageSpread,
                    boolean onlySelf, boolean hideFirstPerson) {
        Minecraft mc = Minecraft.getInstance();
        RenderPipeline pipeline = useDepth ? LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE_LIQUID_IGNORE : LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE;
        Renderer3D.DepthMode dm = useDepth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN;
        MeshBuilder mesh = renderer.batchTextured(pipeline, TextureStorage.FIRE_FLY, dm);
        if (mesh == null) return;
        Quaternionf camRot = RenderState.cameraRotation;
        float s = scale;
        Vector3f right = new Vector3f(1, 0, 0).rotate(camRot).mul(s);
        Vector3f up = new Vector3f(0, 1, 0).rotate(camRot).mul(s);
        float st = Math.max(0.05f, step);
        for (Player player : mc.level.players()) {
            if (onlySelf && player != mc.player) continue;
            if (hideFirstPerson && player == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            List<TrailPoint> trails = ((IEntity) player).lovisual$getTrails();
            int size = trails.size(); if (size == 0) continue;
            for (int i = 0; i < size; i++) {
                TrailPoint ctx = trails.get(i);
                int col = colors.trailPointColor(ctx, i, size, tickDelta, phaseBase, colorMode, colorSpeed, primary, secondary, lengthSpread, ageSpread);
                float anim = ctx.animation(tickDelta);
                int argb = geo.withAlpha(col, (int) (((col >>> 24) & 0xFF) * anim));
                if (connected) {
                    Vec3 from = ctx.from(); Vec3 to = ctx.to();
                    double dist = from.distanceTo(to);
                    int steps = Math.min(CONNECT_MAX_STEPS, (int) Math.ceil(dist / st));
                    if (steps <= 1) {
                        Vec3 pos = ctx.interpolate(tickDelta);
                        geo.addBillboardQuad(mesh, pos.x, pos.y + 0.9, pos.z, right, up, argb);
                    } else {
                        for (int si = 0; si <= steps; si++) {
                            float t = si / (float) steps;
                            Vec3 pos = from.lerp(to, t);
                            geo.addBillboardQuad(mesh, pos.x, pos.y + 0.9, pos.z, right, up, argb);
                        }
                    }
                } else {
                    Vec3 pos = ctx.interpolate(tickDelta);
                    geo.addBillboardQuad(mesh, pos.x, pos.y + 0.9, pos.z, right, up, argb);
                }
            }
        }
    }

    void renderRibbon(Renderer3D renderer, float tickDelta, boolean useDepth,
                      float down, float height, int phaseBase,
                      TrailsColor colors, TrailsGeometry geo,
                      String colorMode, int colorSpeed, int primary, int secondary,
                      int lengthSpread, int ageSpread,
                      boolean onlySelf, boolean hideFirstPerson,
                      boolean stroke, float strokeWidth, float strokeAlpha) {
        Minecraft mc = Minecraft.getInstance();
        RenderPipeline pipeline = useDepth ? LoVisualRenderPipelines.WORLD_COLORED_LIQUID_IGNORE : LoVisualRenderPipelines.WORLD_COLORED;
        Renderer3D.DepthMode dm = useDepth ? Renderer3D.DepthMode.PRE_DEPTH : Renderer3D.DepthMode.MAIN;
        MeshBuilder mesh = renderer.batch(pipeline, dm);
        if (mesh == null) return;
        MeshBuilder strokeMesh = null;
        if (stroke) {
            float prev = RenderState.lineWidth;
            try {
                RenderState.lineWidth = Math.max(0.5f, strokeWidth);
                strokeMesh = renderer.batch(useDepth ? LoVisualRenderPipelines.WORLD_COLORED_LINES_LIQUID_IGNORE : LoVisualRenderPipelines.WORLD_COLORED_LINES, dm);
            } finally { RenderState.lineWidth = prev; }
        }
        for (Player player : mc.level.players()) {
            if (onlySelf && player != mc.player) continue;
            if (hideFirstPerson && player == mc.player && mc.options.getCameraType().isFirstPerson()) continue;
            List<TrailPoint> trails = ((IEntity) player).lovisual$getTrails();
            int size = trails.size(); if (size < 2) continue;
            for (int i = 1; i < size; i++) {
                TrailPoint prev = trails.get(i - 1); TrailPoint cur = trails.get(i);
                Vec3 p0 = prev.interpolate(tickDelta); Vec3 p1 = cur.interpolate(tickDelta);
                int col0 = colors.trailPointColor(prev, i - 1, size, tickDelta, phaseBase, colorMode, colorSpeed, primary, secondary, lengthSpread, ageSpread);
                int col1 = colors.trailPointColor(cur, i, size, tickDelta, phaseBase, colorMode, colorSpeed, primary, secondary, lengthSpread, ageSpread);
                int argb0 = geo.withAlpha(col0, (int) (((col0 >>> 24) & 0xFF) * prev.animation(tickDelta)));
                int argb1 = geo.withAlpha(col1, (int) (((col1 >>> 24) & 0xFF) * cur.animation(tickDelta)));
                geo.addVerticalQuad(mesh, p0, p1, down, height, argb0, argb1);
                if (strokeMesh != null) {
                    int s0 = AnimatedRenderColors.scaleAlpha(argb0, strokeAlpha);
                    int s1 = AnimatedRenderColors.scaleAlpha(argb1, strokeAlpha);
                    geo.addLine(strokeMesh, p0.x, p0.y + down, p0.z, p1.x, p1.y + down, p1.z, s0, s1);
                    geo.addLine(strokeMesh, p0.x, p0.y + down + height, p0.z, p1.x, p1.y + down + height, p1.z, s0, s1);
                }
            }
        }
    }
}
