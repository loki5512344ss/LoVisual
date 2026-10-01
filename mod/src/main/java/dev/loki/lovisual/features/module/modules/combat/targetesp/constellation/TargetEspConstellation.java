package dev.loki.lovisual.features.module.modules.combat.targetesp.constellation;

import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.math.RenderMath;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.function.IntFunction;

import static dev.loki.lovisual.features.module.modules.combat.targetesp.TargetEspRender.*;

/** "Constellation" TargetESP mode: glowing stars orbiting the target, joined by thin lines. */
public final class TargetEspConstellation {
    private static final float LINE_ALPHA = 120.0f / 255.0f;
    private static final float GLOW_SCALE = 3.5f;
    private static final float GLOW_ALPHA = 0.3f;

    private TargetEspConstellation() {
    }

    public static void render(Renderer3D renderer, LivingEntity target, float tickDelta,
                              IntFunction<Integer> palette, int stars, float speed, float orbit,
                              boolean lines, float pointSize, float glow) {
        if (renderer == null || target == null || stars < 2) return;

        Vec3 pos = RenderMath.getLerpedPos(target, tickDelta);
        double time = (GameClock.millis() % 100000L) / 1000.0;
        double[] p = new double[stars * 3];
        ConstellationLayout.positions(p, stars, target.getBbWidth(), target.getBbHeight(), orbit, time, speed);

        if (lines) {
            drawLines(renderer, pos, p, stars, palette);
        }
        drawStars(renderer, pos, p, stars, time, palette, pointSize, glow);
    }

    private static void drawLines(Renderer3D renderer, Vec3 pos, double[] p, int stars,
                                  IntFunction<Integer> palette) {
        float prevWidth = RenderState.lineWidth;
        RenderState.lineWidth = 1.2f;
        MeshBuilder mesh;
        try {
            mesh = renderer.batch(LoVisualRenderPipelines.WORLD_COLORED_LINES, Renderer3D.DepthMode.NONE);
        } finally {
            RenderState.lineWidth = prevWidth;
        }
        if (mesh == null) return;

        for (int s = 0; s < stars; s++) {
            int n = (s + 1) % stars;
            int color = applyOpacity(palette.apply((int) (360.0 * s / stars)), LINE_ALPHA);
            mesh.ensureLineCapacity();
            int a = addVertex(mesh, pos.x + p[s * 3], pos.y + p[s * 3 + 1], pos.z + p[s * 3 + 2], color);
            int b = addVertex(mesh, pos.x + p[n * 3], pos.y + p[n * 3 + 1], pos.z + p[n * 3 + 2], color);
            mesh.line(a, b);
        }
    }

    private static void drawStars(Renderer3D renderer, Vec3 pos, double[] p, int stars, double time,
                                  IntFunction<Integer> palette, float pointSize, float glow) {
        MeshBuilder core = renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE,
                TextureStorage.PARTICLE_STARS, Renderer3D.DepthMode.MAIN);
        MeshBuilder halo = glow > 0.01f
                ? renderer.batchTextured(LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE,
                TextureStorage.BLOOM, Renderer3D.DepthMode.MAIN)
                : null;
        if (core == null) return;

        for (int s = 0; s < stars; s++) {
            float pulse = ConstellationLayout.pulse(time, s, stars);
            float alpha = 0.7f + 0.3f * pulse;
            int color = palette.apply((int) (360.0 * s / stars));
            double x = pos.x + p[s * 3];
            double y = pos.y + p[s * 3 + 1];
            double z = pos.z + p[s * 3 + 2];
            float size = pointSize * pulse;
            addBillboardQuad(core, x, y, z, size, applyOpacity(color, alpha));
            if (halo != null) {
                addBillboardQuad(halo, x, y, z, size * GLOW_SCALE, applyOpacity(color, alpha * glow * GLOW_ALPHA));
            }
        }
    }
}
