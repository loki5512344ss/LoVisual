package dev.loki.lovisual.features.module.modules.visuals.effects.hits;

import dev.loki.lovisual.render.engine.RenderState;
import dev.loki.lovisual.render.engine.color.ColorUtils;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.pipeline.LoVisualRenderPipelines;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.texture.TextureStorage;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.util.time.Timer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class KillEmberHelper {
    private static final int MAX_EMBERS = 26;
    private static final int MIN_EMBERS = 16;
    private static final long EMBER_GROUND_EXTRA_MS = 0;

    private final Random random = new Random();
    private final List<Ember> embers = new ArrayList<>();
    private final List<FlashRing> flashes = new ArrayList<>();

    void spawn(Vec3 pos) {
        int count = MIN_EMBERS + random.nextInt(MAX_EMBERS - MIN_EMBERS + 1);
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 0.18 + random.nextDouble() * 0.18;
            double vx = Math.cos(angle) * speed;
            double vz = Math.sin(angle) * speed;
            double vy = 0.28 + random.nextDouble() * 0.20;
            long life = 4000 + random.nextInt(401);
            float size = 0.24f + random.nextFloat() * 0.18f;
            embers.add(new Ember(pos, new Vec3(vx, vy, vz), life, size));
        }
        flashes.add(new FlashRing(pos, 240));
    }

    void tick(ClientLevel level) {
        if (!embers.isEmpty()) {
            embers.removeIf(e -> e.update(level));
        }
        if (!flashes.isEmpty()) {
            flashes.removeIf(f -> f.timer.passedMs(f.lifeMs));
        }
    }

    void render(Renderer3D renderer, float tickDelta) {
        renderEmbers(renderer, tickDelta);
        renderFlashRings(renderer);
    }

    void clear() {
        embers.clear();
        flashes.clear();
    }

    private void renderEmbers(Renderer3D renderer, float tickDelta) {
        if (embers.isEmpty()) {
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
        for (Ember e : embers) {
            Vec3 pos = e.interpolate(tickDelta);
            float lifeT = e.getLifeProgress();
            Color base = new Color(255, 20, 0, 255);
            Color hot = new Color(255, 220, 80, 255);
            Color col = ColorUtils.interpolateColorC(base, hot, 1f - lifeT);
            int alpha = (int) (255f * (1f - lifeT));
            int argb = withAlpha(col.getRGB(), alpha);
            float size = e.size * (1f - lifeT);
            if (size <= 0.01f) {
                continue;
            }
            HitRenderUtil.addBillboardQuad(mesh, pos.x, pos.y, pos.z, size, camRot, argb);
        }
    }

    private void renderFlashRings(Renderer3D renderer) {
        if (flashes.isEmpty()) {
            return;
        }
        MeshBuilder mesh = renderer.batchTextured(
                LoVisualRenderPipelines.WORLD_TEXTURED_ADDITIVE,
                TextureStorage.DEFAULT_CIRCLE,
                Renderer3D.DepthMode.MAIN
        );
        if (mesh == null) {
            return;
        }
        for (FlashRing ring : flashes) {
            float t = Math.min(1f, ring.timer.getPassedTimeMs() / (float) ring.lifeMs);
            float size = 0.4f + t * 1.8f;
            float alpha = (float) Math.pow(1f - t, 1.6);
            Color base = new Color(255, 60, 0, 255);
            Color hot = new Color(255, 220, 120, 255);
            Color col = ColorUtils.interpolateColorC(hot, base, t);
            int argb = withAlpha(col.getRGB(), (int) (255f * alpha));
            float angle = t * 180f;
            addGroundQuad(mesh, ring.pos, size, angle, argb);
        }
    }

    private static void addGroundQuad(MeshBuilder mesh, Vec3 center, float size, float angleDeg, int argb) {
        double radians = Math.toRadians(angleDeg);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);
        Vec3 right = new Vec3(cos, 0.0, -sin);
        Vec3 forward = new Vec3(sin, 0.0, cos);
        Vec3 p1 = center.add(right.scale(-size)).add(forward.scale(size));
        Vec3 p2 = center.add(right.scale(size)).add(forward.scale(size));
        Vec3 p3 = center.add(right.scale(size)).add(forward.scale(-size));
        Vec3 p4 = center.add(right.scale(-size)).add(forward.scale(-size));
        mesh.ensureQuadCapacity();
        int i1 = mesh.vec3(p1.x, p1.y, p1.z).vec2(0, 1).color(new RenderColor(argb)).next();
        int i2 = mesh.vec3(p2.x, p2.y, p2.z).vec2(1, 1).color(new RenderColor(argb)).next();
        int i3 = mesh.vec3(p3.x, p3.y, p3.z).vec2(1, 0).color(new RenderColor(argb)).next();
        int i4 = mesh.vec3(p4.x, p4.y, p4.z).vec2(0, 0).color(new RenderColor(argb)).next();
        mesh.quad(i1, i2, i3, i4);
    }

    private static int withAlpha(int argb, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        return (a << 24) | (argb & 0x00FFFFFF);
    }

    private static class Ember {
        private final Timer timer;
        private final long lifeMs;
        private final float size;
        private Vec3 pos;
        private Vec3 prevPos;
        private Vec3 velocity;
        private boolean grounded;
        private long groundExtraMs;

        private Ember(Vec3 pos, Vec3 velocity, long lifeMs, float size) {
            this.pos = pos;
            this.prevPos = pos;
            this.velocity = velocity;
            this.timer = new Timer();
            this.lifeMs = lifeMs;
            this.size = size;
        }

        private boolean update(ClientLevel world) {
            prevPos = pos;
            pos = pos.add(velocity);
            if (!grounded && world != null) {
                var blockPos = net.minecraft.core.BlockPos.containing(pos.x, pos.y - 0.02, pos.z);
                boolean solid = !world.getBlockState(blockPos).isAir();
                if (solid) {
                    grounded = true;
                    velocity = new Vec3(0.0, 0.0, 0.0);
                    groundExtraMs = EMBER_GROUND_EXTRA_MS;
                    pos = new Vec3(pos.x, blockPos.getY() + 1.001, pos.z);
                }
            }
            if (!grounded) {
                velocity = velocity.multiply(0.97, 0.97, 0.97).add(0.0, -0.055, 0.0);
            }
            long totalLife = lifeMs + groundExtraMs;
            return timer.passedMs(totalLife);
        }

        private Vec3 interpolate(float tickDelta) {
            return prevPos.lerp(pos, tickDelta);
        }

        private float getLifeProgress() {
            long totalLife = lifeMs + groundExtraMs;
            return Math.min(1f, timer.getPassedTimeMs() / (float) totalLife);
        }
    }

    private static class FlashRing {
        private final Vec3 pos;
        private final Timer timer;
        private final long lifeMs;

        private FlashRing(Vec3 pos, long lifeMs) {
            this.pos = pos;
            this.timer = new Timer();
            this.lifeMs = lifeMs;
        }
    }
}
