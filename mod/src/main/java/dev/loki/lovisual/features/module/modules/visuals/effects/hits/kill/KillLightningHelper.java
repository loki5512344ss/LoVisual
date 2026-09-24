package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.GameClock;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class KillLightningHelper {
    private static final int LIGHTNING_POINTS = 200;
    private static final double LIGHTNING_STEP_Y = 0.25;
    private static final float LIGHTNING_STEP_SPREAD = 0.25f;
    private static final long LIGHTNING_APPEAR_MS = 1200L;
    private static final long LIGHTNING_FADE_MS = 250L;
    private static final long LIGHTNING_LIFE_MS = LIGHTNING_APPEAR_MS + LIGHTNING_FADE_MS;
    private static final float LIGHTNING_MIN_SIZE = 0.26f;
    private static final float LIGHTNING_MAX_SIZE = 0.78f;
    private static final float LIGHTNING_ALPHA = 0.40f;

    private final Random random = new Random();
    private final List<LightningStrike> strikes = new ArrayList<>();

    void add(Vec3 pos, int colorArgb) {
        strikes.add(new LightningStrike(pos, colorArgb, GameClock.millis()));
    }

    void tick() {
        if (!strikes.isEmpty()) {
            strikes.removeIf(LightningStrike::isExpired);
        }
    }

    void render(Renderer3D renderer) {
        if (strikes.isEmpty()) {
            return;
        }
        MeshBuilder mesh = renderer.batchTextured(
                LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE,
                TextureStorage.FIRE_FLY,
                Renderer3D.DepthMode.MAIN
        );
        if (mesh == null) {
            return;
        }
        Quaternionf camRot = RenderState.cameraRotation;
        for (LightningStrike strike : strikes) {
            float alpha = strike.getAlpha();
            if (alpha <= 0.01f) {
                continue;
            }
            int colorArgb = withAlpha(strike.colorArgb, (int) (255f * alpha * LIGHTNING_ALPHA));
            int pointCount = Math.max(1, strike.points.size() - 1);
            for (int i = 0; i < strike.points.size(); i++) {
                float progress = i / (float) pointCount;
                float size = LIGHTNING_MIN_SIZE + (LIGHTNING_MAX_SIZE - LIGHTNING_MIN_SIZE) * progress;
                Vec3 point = strike.points.get(i);
                HitRenderUtil.addBillboardQuad(mesh, point.x, point.y, point.z, size, camRot, colorArgb);
            }
        }
    }

    void clear() {
        strikes.clear();
    }

    private static int withAlpha(int argb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static float smoothProgress(float value) {
        float clamped = Math.max(0f, Math.min(1f, value));
        return clamped * clamped * (3f - 2f * clamped);
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private class LightningStrike {
        private final List<Vec3> points = new ArrayList<>(LIGHTNING_POINTS);
        private final int colorArgb;
        private final long createdAtMs;

        private LightningStrike(Vec3 startPos, int colorArgb, long createdAtMs) {
            this.colorArgb = colorArgb;
            this.createdAtMs = createdAtMs;
            Vec3 current = startPos;
            for (int i = 0; i < LIGHTNING_POINTS; i++) {
                current = current.add(
                        randomRange(-LIGHTNING_STEP_SPREAD, LIGHTNING_STEP_SPREAD),
                        LIGHTNING_STEP_Y,
                        randomRange(-LIGHTNING_STEP_SPREAD, LIGHTNING_STEP_SPREAD)
                );
                points.add(current);
            }
        }

        private boolean isExpired() {
            return GameClock.millis() - createdAtMs > LIGHTNING_LIFE_MS;
        }

        private float getAlpha() {
            long age = GameClock.millis() - createdAtMs;
            float appear = smoothProgress(Math.min(1f, age / (float) LIGHTNING_APPEAR_MS));
            float fade = 1f;
            if (age > LIGHTNING_APPEAR_MS) {
                float fadeProgress = Math.min(1f, (age - LIGHTNING_APPEAR_MS) / (float) LIGHTNING_FADE_MS);
                fade = 1f - smoothProgress(fadeProgress);
            }
            return Math.max(0f, Math.min(1f, appear * fade));
        }
    }
}
