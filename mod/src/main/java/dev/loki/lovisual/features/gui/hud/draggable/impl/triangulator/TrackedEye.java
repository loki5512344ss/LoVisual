package dev.loki.lovisual.features.gui.hud.draggable.impl.triangulator;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Rolling observation window for one thrown eye of ender plus the
 * quality-scored capture computed once its trajectory stabilizes.
 */
public final class TrackedEye {
    private static final double MIN_HORIZONTAL_SPEED = 0.03;
    private static final int MAX_OBSERVATIONS = 14;

    private final List<Observation> observations = new ArrayList<>();
    private long lastSeenMs;
    private boolean captured;

    TrackedEye(int entityId) {
    }

    public void observe(EyeOfEnder eye, long now) {
        if (eye == null) {
            return;
        }
        Vec3 velocity = eye.getDeltaMovement();
        double speed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (speed < MIN_HORIZONTAL_SPEED) {
            lastSeenMs = now;
            return;
        }
        observations.add(new Observation(eye.getX(), eye.getZ(), velocity.x / speed, velocity.z / speed, speed));
        if (observations.size() > MAX_OBSERVATIONS) {
            observations.remove(0);
        }
        lastSeenMs = now;
    }

    public TrackedCapture tryCapture(int minTicks, double stabilityThreshold, double deviationLimit) {
        if (captured || observations.size() < Math.max(2, minTicks)) {
            return null;
        }

        double dirX = 0.0;
        double dirZ = 0.0;
        double avgSpeed = 0.0;
        for (Observation observation : observations) {
            dirX += observation.dirX();
            dirZ += observation.dirZ();
            avgSpeed += observation.speed();
        }
        avgSpeed /= observations.size();

        double dirLen = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (dirLen <= 1.0e-6) {
            return null;
        }
        dirX /= dirLen;
        dirZ /= dirLen;

        double centroidX = 0.0;
        double centroidZ = 0.0;
        double avgCos = 0.0;
        double minCos = 1.0;
        for (Observation observation : observations) {
            centroidX += observation.x();
            centroidZ += observation.z();
            double dot = Mth.clamp(observation.dirX() * dirX + observation.dirZ() * dirZ, -1.0, 1.0);
            avgCos += dot;
            minCos = Math.min(minCos, dot);
        }
        centroidX /= observations.size();
        centroidZ /= observations.size();
        avgCos /= observations.size();

        double normalX = -dirZ;
        double normalZ = dirX;
        double deviation = 0.0;
        for (Observation observation : observations) {
            deviation += Math.abs((observation.x() - centroidX) * normalX + (observation.z() - centroidZ) * normalZ);
        }
        deviation /= observations.size();

        if (avgCos < stabilityThreshold || deviation > deviationLimit) {
            return null;
        }

        double stabilityScore = Mth.clamp(
                (avgCos - stabilityThreshold) / Math.max(1.0e-5, 1.0 - stabilityThreshold),
                0.0,
                1.0
        );
        double minCosScore = Mth.clamp(
                (minCos - (stabilityThreshold - 0.01)) / Math.max(1.0e-5, 1.01 - stabilityThreshold),
                0.0,
                1.0
        );
        double deviationScore = Math.exp(-deviation / 0.15);
        double speedScore = Mth.clamp(avgSpeed / 0.35, 0.0, 1.0);
        double quality = Mth.clamp(
                stabilityScore * 0.40 + minCosScore * 0.25 + deviationScore * 0.20 + speedScore * 0.15,
                0.0,
                1.0
        );

        return new TrackedCapture(
                centroidX,
                centroidZ,
                dirX,
                dirZ,
                quality,
                avgCos,
                deviation,
                observations.size()
        );
    }

    public boolean captured() {
        return captured;
    }

    public void markCaptured() {
        captured = true;
    }

    public long lastSeenMs() {
        return lastSeenMs;
    }

    public record Observation(double x, double z, double dirX, double dirZ, double speed) {
    }

    public record TrackedCapture(double pointX,
                                 double pointZ,
                                 double dirX,
                                 double dirZ,
                                 double quality,
                                 double stability,
                                 double deviation,
                                 int samples) {
    }
}
