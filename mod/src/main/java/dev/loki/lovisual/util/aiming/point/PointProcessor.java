package dev.loki.lovisual.util.aiming.point;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.data.Rotation;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Holds tunable aim-behaviour options and applies the post-selection point pipeline
 * (delay, lazy hold, gaussian jitter, motion instability) for a single tracked entity.
 * Package-private: only {@link PointTracker} composes it.
 */
final class PointProcessor {
    private boolean delayEnabled = true;
    private int delayMin = 2;
    private int delayMax = 4;
    private boolean lazyEnabled = true;
    private double lazyMin = 0.10;
    private double lazyMax = 0.20;
    private boolean gaussianEnabled;
    private double gaussianYawFactor;
    private double gaussianPitchFactor;
    private int gaussianChance = 100;
    private double gaussianSpeed = 0.15;
    private PointInsideBox delayedPoint;
    private int currentDelay = PointGeometry.randomInt(2, 4);
    private PointInsideBox lazyPoint;
    private double currentLazyThreshold;
    private Vec3 currentOffset = Vec3.ZERO;
    private Vec3 targetOffset = Vec3.ZERO;
    private Vec3 lastEyes;
    private Vec3 lastAimDirection;
    private Vec3 instabilityOffset = Vec3.ZERO;
    private Vec3 targetInstabilityOffset = Vec3.ZERO;
    private int instabilityRetargetTicks;
    /**
     * Replaces all runtime options in one shot; clamps each field so min/max pairs stay consistent.
     * Called by {@link PointTracker#setOptions}.
     */
    void setOptions(boolean delayEnabled, int delayMin, int delayMax,
                    boolean lazyEnabled, double lazyMin, double lazyMax,
                    boolean gaussianEnabled, double gaussianYawFactor, double gaussianPitchFactor,
                    int gaussianChance, double gaussianSpeed) {
        this.delayEnabled = delayEnabled;
        this.delayMin = Math.max(0, Math.min(delayMin, delayMax));
        this.delayMax = Math.max(this.delayMin, delayMax);
        this.lazyEnabled = lazyEnabled;
        this.lazyMin = Math.max(0.0, Math.min(lazyMin, lazyMax));
        this.lazyMax = Math.max(this.lazyMin, lazyMax);
        this.gaussianEnabled = gaussianEnabled;
        this.gaussianYawFactor = Math.max(0.0, gaussianYawFactor);
        this.gaussianPitchFactor = Math.max(0.0, gaussianPitchFactor);
        this.gaussianChance = Math.max(0, Math.min(100, gaussianChance));
        this.gaussianSpeed = Math.max(0.01, Math.min(1.0, gaussianSpeed));
    }
    /** Clears every per-target counter so a new entity starts with fresh delay / lazy / instability state. */
    void reset() {
        delayedPoint = null;
        currentDelay = PointGeometry.randomInt(delayMin, delayMax);
        lazyPoint = null;
        currentLazyThreshold = 0.0;
        currentOffset = Vec3.ZERO;
        targetOffset = Vec3.ZERO;
        lastEyes = null;
        lastAimDirection = null;
        instabilityOffset = Vec3.ZERO;
        targetInstabilityOffset = Vec3.ZERO;
        instabilityRetargetTicks = 0;
    }
    /** Runs the selected point through delay -> lazy -> gaussian -> motion-instability filters. */
    PointInsideBox process(PointInsideBox point, Vec3 eyes, Entity entity, Rotation initialRotation) {
        point = applyDelay(point);
        point = applyLazy(point);
        point = applyGaussian(point);
        return applyMotionInstability(point, eyes, entity, initialRotation);
    }
    /** Keeps the previously emitted point for a random short window before switching to the new one. */
    private PointInsideBox applyDelay(PointInsideBox point) {
        if (!delayEnabled) {
            delayedPoint = point;
            currentDelay = 0;
            return point;
        }
        if (delayedPoint != null) {
            delayedPoint = new PointInsideBox(PointGeometry.clampToBox(delayedPoint.pos(), point.box()), point.box());
        }
        if (point.equals(delayedPoint)) return point;
        if (delayedPoint == null) {
            delayedPoint = point;
            currentDelay = PointGeometry.randomInt(delayMin, delayMax);
            return point;
        }
        PointInsideBox currentPoint = delayedPoint;
        currentDelay--;
        if (currentDelay > 0) return currentPoint;
        delayedPoint = point;
        currentDelay = PointGeometry.randomInt(delayMin, delayMax);
        return currentPoint;
    }
    /** Snaps the emitted point back to the previous one until the raw movement exceeds a lazy threshold. */
    private PointInsideBox applyLazy(PointInsideBox point) {
        if (!lazyEnabled) {
            lazyPoint = point;
            currentLazyThreshold = 0.0;
            return point;
        }
        if (lazyPoint != null) {
            lazyPoint = new PointInsideBox(PointGeometry.clampToBox(lazyPoint.pos(), point.box()), point.box());
        }
        if (lazyPoint == null) {
            lazyPoint = point;
            currentLazyThreshold = PointGeometry.randomDouble(lazyMin, lazyMax);
            return point;
        }
        PointInsideBox currentPoint = lazyPoint;
        double thresholdSq = currentLazyThreshold * currentLazyThreshold;
        if (point.pos().distanceToSqr(currentPoint.pos()) < thresholdSq) return currentPoint;
        lazyPoint = point;
        currentLazyThreshold = PointGeometry.randomDouble(lazyMin, lazyMax);
        return currentPoint;
    }
    /** Adds a slowly-lerping Gaussian offset that occasionally re-targets (per {@code gaussianChance}% of ticks). */
    private PointInsideBox applyGaussian(PointInsideBox point) {
        if (!gaussianEnabled || (gaussianYawFactor <= 0.0 && gaussianPitchFactor <= 0.0)) {
            currentOffset = Vec3.ZERO;
            targetOffset = Vec3.ZERO;
            return point;
        }
        if (hasReachedOffsetTarget()) {
            if (ThreadLocalRandom.current().nextInt(100) < gaussianChance) {
                targetOffset = new Vec3(
                        ThreadLocalRandom.current().nextGaussian() * gaussianYawFactor,
                        ThreadLocalRandom.current().nextGaussian() * gaussianPitchFactor,
                        ThreadLocalRandom.current().nextGaussian() * gaussianYawFactor
                );
            } else {
                targetOffset = Vec3.ZERO;
            }
        } else {
            currentOffset = new Vec3(
                    Mth.lerp(gaussianSpeed, currentOffset.x, targetOffset.x),
                    Mth.lerp(gaussianSpeed, currentOffset.y, targetOffset.y),
                    Mth.lerp(gaussianSpeed, currentOffset.z, targetOffset.z)
            );
        }
        return new PointInsideBox(point.pos().add(currentOffset), point.box());
    }
    /** Biases the aim point outward when the player turns fast, moves, or the target itself is in motion. */
    private PointInsideBox applyMotionInstability(PointInsideBox point, Vec3 eyes, Entity entity, Rotation initialRotation) {
        Vec3 aimDirection = initialRotation.directionVector().normalize();
        double angleChange = lastAimDirection != null
                ? Math.toDegrees(Math.acos(Mth.clamp(lastAimDirection.dot(aimDirection), -1.0, 1.0)))
                : 0.0;
        double playerMove = lastEyes != null ? eyes.distanceTo(lastEyes) : 0.0;
        Vec3 entityVelocity = entity != null ? entity.getDeltaMovement() : Vec3.ZERO;
        double entityMove = PointGeometry.horizontalLength(entityVelocity) + Math.abs(entityVelocity.y) * 0.45;
        lastEyes = eyes;
        lastAimDirection = aimDirection;
        double intensity = Mth.clamp(angleChange / 14.0 + playerMove / 0.14 + entityMove / 0.11, 0.0, 1.0);
        if (intensity < 0.12) {
            instabilityOffset = instabilityOffset.scale(0.72);
            targetInstabilityOffset = targetInstabilityOffset.scale(0.72);
            return new PointInsideBox(PointGeometry.clampToBox(point.pos().add(instabilityOffset), point.box()), point.box());
        }
        instabilityRetargetTicks--;
        if (instabilityRetargetTicks <= 0 || instabilityOffset.distanceToSqr(targetInstabilityOffset) < 1.0E-4) {
            targetInstabilityOffset = PointGeometry.randomInstabilityOffset(point.box(), entityVelocity, aimDirection, intensity);
            instabilityRetargetTicks = PointGeometry.randomInt(2, intensity > 0.65 ? 4 : 6);
        }
        double blend = 0.14 + 0.18 * intensity;
        instabilityOffset = new Vec3(
                Mth.lerp(blend, instabilityOffset.x, targetInstabilityOffset.x),
                Mth.lerp(blend, instabilityOffset.y, targetInstabilityOffset.y),
                Mth.lerp(blend, instabilityOffset.z, targetInstabilityOffset.z)
        );
        return new PointInsideBox(PointGeometry.clampToBox(point.pos().add(instabilityOffset), point.box()), point.box());
    }
    private boolean hasReachedOffsetTarget() {
        return Math.abs(currentOffset.x - targetOffset.x) < 0.01
                && Math.abs(currentOffset.y - targetOffset.y) < 0.01
                && Math.abs(currentOffset.z - targetOffset.z) < 0.01;
    }
}
