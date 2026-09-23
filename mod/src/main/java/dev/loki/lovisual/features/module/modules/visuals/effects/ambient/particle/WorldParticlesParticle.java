package dev.loki.lovisual.features.module.modules.visuals.effects.ambient;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.util.time.GameClock;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Состояние одной частицы WorldParticles: позиция, вращение, дрейф и
 * анимации прозрачности. Спавн — {@link #spawn}, физика — {@link #tick()}.
 */
final class WorldParticlesParticle {
    private static final float MOTION_SCALE = 0.04f;
    private static final float DRAG = 0.98f;
    private static final long ALPHA_ANIM_MS = 1000L;

    private final long spawnMs;
    private final long lifeMs;
    private final float size;
    private final float visualPhase;
    private final float eyeSpin;
    private final float distortionSpin;
    private Vec3 prevPos;
    private Vec3 pos;
    private Vec3 prevRotation;
    private Vec3 rotation;
    private Vec3 motion;
    private Vec3 rotationMotion;

    private WorldParticlesParticle(Vec3 pos, Vec3 motion, Vec3 rotationMotion, long lifeMs, float size,
                                   float visualPhase, float eyeSpin, float distortionSpin) {
        this.prevPos = pos;
        this.pos = pos;
        this.prevRotation = Vec3.ZERO;
        this.rotation = Vec3.ZERO;
        this.motion = motion;
        this.rotationMotion = rotationMotion;
        this.spawnMs = GameClock.millis();
        this.lifeMs = Math.max(1L, lifeMs);
        this.size = size;
        this.visualPhase = visualPhase;
        this.eyeSpin = eyeSpin;
        this.distortionSpin = distortionSpin;
    }

    static WorldParticlesParticle spawn(double radius, double height, int lifeMinMs, int lifeMaxMs,
                                        double sizeMin, double sizeMax, boolean bubbleMode) {
        Minecraft mc = Minecraft.getInstance();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Vec3 playerPos = mc.player.position();

        double range = Math.max(0.1, radius);
        double heightRange = Math.max(0.1, height);
        double px = playerPos.x + random.nextDouble(-range, range);
        double py = playerPos.y + random.nextDouble(0.0, heightRange);
        double pz = playerPos.z + random.nextDouble(-range, range);

        Vec3 pos = new Vec3(px, py, pz);
        Vec3 motion = bubbleMode
                ? new Vec3(
                        random.nextDouble(-0.42, 0.42) * MOTION_SCALE,
                        random.nextDouble(1.10, 2.55) * MOTION_SCALE,
                        random.nextDouble(-0.42, 0.42) * MOTION_SCALE
                )
                : new Vec3(
                        random.nextDouble(-1.0, 1.0) * MOTION_SCALE,
                        random.nextDouble(0.0, 2.0) * MOTION_SCALE,
                        random.nextDouble(-1.0, 1.0) * MOTION_SCALE
                );
        Vec3 rotationMotion = new Vec3(
                random.nextDouble(-1.0, 1.0) * MOTION_SCALE,
                random.nextDouble(-1.0, 1.0) * MOTION_SCALE,
                random.nextDouble(-1.0, 1.0) * MOTION_SCALE
        );

        int minLife = Math.max(1, Math.min(lifeMinMs, lifeMaxMs));
        int maxLife = Math.max(minLife, Math.max(lifeMinMs, lifeMaxMs));
        long life = randomLongClosed(random, minLife, maxLife);

        double minSize = Math.max(0.02, Math.min(sizeMin, sizeMax));
        double maxSize = Math.max(minSize, Math.max(sizeMin, sizeMax));
        float size = (float) randomDoubleClosed(random, minSize, maxSize);

        float visualPhase = (float) random.nextDouble(0.0, Math.PI * 2.0);
        float eyeSpin = (float) random.nextDouble(0.38, 0.72);
        float distortionSpin = (float) random.nextDouble(-0.28, -0.12);

        return new WorldParticlesParticle(pos, motion, rotationMotion, life, size, visualPhase, eyeSpin, distortionSpin);
    }

    void tick() {
        prevPos = pos;
        prevRotation = rotation;
        pos = pos.add(motion);
        rotation = rotation.add(rotationMotion);
        motion = motion.scale(DRAG);
        rotationMotion = rotationMotion.scale(DRAG);
    }

    float alpha() {
        long age = GameClock.millis() - spawnMs;
        if (age <= 0L) {
            return 0.0f;
        }
        if (age < ALPHA_ANIM_MS) {
            return AnimationUtility.smoothstep(age / (float) ALPHA_ANIM_MS);
        }
        if (age < lifeMs) {
            return 1.0f;
        }
        float fade = 1.0f - (age - lifeMs) / (float) ALPHA_ANIM_MS;
        return AnimationUtility.smoothstep(Mth.clamp(fade, 0.0f, 1.0f));
    }

    float funnelAlpha() {
        long age = ageMs();
        if (age <= 0L) {
            return 0.0f;
        }

        float appear = AnimationUtility.easeOutCubic(age / 720.0f);
        float disappear = 1.0f;
        if (age > lifeMs) {
            disappear = 1.0f - AnimationUtility.easeInCubic((age - lifeMs) / (float) ALPHA_ANIM_MS);
        }

        float breath = 0.88f + 0.12f * (float) Math.sin(age * 0.0032f + visualPhase);
        return AnimationUtility.clamp01(appear * disappear * breath);
    }

    float funnelScale() {
        long age = ageMs();
        float appear = AnimationUtility.easeOutCubic(age / 900.0f);
        float disappear = 1.0f;
        if (age > lifeMs) {
            disappear = 1.0f - AnimationUtility.smoothstep((age - lifeMs) / (float) ALPHA_ANIM_MS);
        }

        float open = AnimationUtility.lerp(0.68f, 1.04f, appear);
        float close = AnimationUtility.lerp(0.82f, 1.0f, disappear);
        return open * close;
    }

    long ageMs() {
        return GameClock.millis() - spawnMs;
    }

    float lifeProgress() {
        return AnimationUtility.clamp01(ageMs() / (float) Math.max(1L, lifeMs));
    }

    boolean isExpired() {
        return GameClock.millis() - spawnMs >= lifeMs + ALPHA_ANIM_MS;
    }

    Vec3 interpolatePos(float tickDelta) {
        return prevPos.lerp(pos, tickDelta);
    }

    Vec3 interpolateRotation(float tickDelta) {
        return prevRotation.lerp(rotation, tickDelta);
    }

    float size() {
        return size;
    }

    float visualPhase() {
        return visualPhase;
    }

    float eyeSpin() {
        return eyeSpin;
    }

    float distortionSpin() {
        return distortionSpin;
    }

    private static double randomDoubleClosed(ThreadLocalRandom random, double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max)) return 0.02;
        double low = Math.min(min, max);
        double high = Math.max(min, max);
        return high <= low ? low : random.nextDouble(low, high);
    }

    private static long randomLongClosed(ThreadLocalRandom random, long min, long max) {
        long low = Math.min(min, max);
        long high = Math.max(min, max);
        if (high <= low) return low;
        if (high == Long.MAX_VALUE) return random.nextLong(low, Long.MAX_VALUE) + 1L;
        return random.nextLong(low, high + 1L);
    }
}
