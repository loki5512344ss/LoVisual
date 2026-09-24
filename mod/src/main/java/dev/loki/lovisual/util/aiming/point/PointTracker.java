package dev.loki.lovisual.util.aiming.point;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.entity.simulation.PositionExtrapolation;

import java.util.ArrayList;
import java.util.List;

/**
 * Chooses and stabilises an aim point on a target's hitbox across ticks.
 * Public entry point of the point-tracking util: callers invoke {@link #findPoint} every tick
 * and configure behaviour via {@link #setOptions}.
 *
 * <p>Internal layout: pure geometry helpers live in {@link PointGeometry}; the stateful
 * post-selection pipeline (delay, lazy hold, gaussian jitter, motion instability) lives in
 * {@link PointProcessor}. This class keeps only cross-tick selection state and continuity.</p>
 */
public final class PointTracker {
    private final PointProcessor processor = new PointProcessor();
    private int trackedEntityId = -1;
    private Vec3 smoothedPoint;
    private Vec3 lastSelectedPoint;
    /** Applies runtime tuning for the point-processing pipeline. See {@link PointProcessor#setOptions}. */
    public void setOptions(boolean delayEnabled,
                           int delayMin,
                           int delayMax,
                           boolean lazyEnabled,
                           double lazyMin,
                           double lazyMax,
                           boolean gaussianEnabled,
                           double gaussianYawFactor,
                           double gaussianPitchFactor,
                           int gaussianChance,
                           double gaussianSpeed) {
        processor.setOptions(delayEnabled, delayMin, delayMax, lazyEnabled, lazyMin, lazyMax,
                gaussianEnabled, gaussianYawFactor, gaussianPitchFactor, gaussianChance, gaussianSpeed);
    }
    /**
     * Computes the aim point for the entity's hitbox extrapolated {@code ticks} forward,
     * honouring max-distance and wall visibility constraints.
     */
    public PointInsideBox findPoint(Vec3 eyes,
                                    Entity entity,
                                    int ticks,
                                    Rotation initialRotation,
                                    double maxDistance,
                                    boolean ignoreWalls) {
        AABB box = PositionExtrapolation.getBestForEntity(entity).getBoxInTicks(ticks);
        List<Vec3> points = PointGeometry.projectPointsOnBox(eyes, box);
        if (trackedEntityId != entity.getId()) {
            reset();
            trackedEntityId = entity.getId();
        }
        Vec3 bestPoint = selectMultiPoint(eyes, entity, box, initialRotation, maxDistance, ignoreWalls);
        if (bestPoint == null) {
            Vec3 anchor = smoothedPoint != null ? PointGeometry.clampToBox(smoothedPoint, box) : eyes;
            bestPoint = points.isEmpty() ? PointGeometry.getNearestPoint(box, anchor) : points.get(0);
            for (Vec3 point : points) {
                if (point.distanceToSqr(anchor) < bestPoint.distanceToSqr(anchor)) {
                    bestPoint = point;
                }
            }
        }
        PointInsideBox point = new PointInsideBox(PointGeometry.clampToBox(bestPoint, box), box);
        point = processor.process(point, eyes, entity, initialRotation);
        point = applyContinuity(point);
        smoothedPoint = point.pos();
        return new PointInsideBox(PointGeometry.clampToBox(point.pos(), box), box);
    }
    /** Convenience overload with unlimited range and walls ignored. */
    public PointInsideBox findPoint(Vec3 eyes, Entity entity, int ticks, Rotation initialRotation) {
        return findPoint(eyes, entity, ticks, initialRotation, Double.MAX_VALUE, true);
    }
    /** Forgets the tracked entity and every per-target counter (selection, delay, lazy, gaussian, instability). */
    public void reset() {
        trackedEntityId = -1;
        smoothedPoint = null;
        lastSelectedPoint = null;
        processor.reset();
    }
    /**
     * Scores every projected box point (plus the aim-ray hit) and picks one from a
     * diverse, weighted pool. Falls back to {@code null} when no valid candidate exists.
     */
    private Vec3 selectMultiPoint(Vec3 eyes,
                                  Entity entity,
                                  AABB box,
                                  Rotation initialRotation,
                                  double maxDistance,
                                  boolean ignoreWalls) {
        if (eyes == null || box == null || initialRotation == null) {
            return null;
        }
        List<PointGeometry.Candidate> candidates = new ArrayList<>();
        float bestScore = Float.MAX_VALUE;
        Vec3 center = box.getCenter();
        Vec3 aimDir = initialRotation.directionVector().normalize();
        Vec3 movement = entity != null ? entity.getDeltaMovement() : Vec3.ZERO;
        double movementStrength = PointGeometry.horizontalLength(movement);
        Vec3 rayHit = PointGeometry.findRayHitPoint(eyes, aimDir, box, maxDistance);
        if (rayHit != null && PointGeometry.isValidPoint(eyes, rayHit, maxDistance, ignoreWalls)) {
            Rotation rayRotation = Rotation.lookingAt(rayHit, eyes).normalize();
            float score = PointGeometry.scoreCandidate(eyes, initialRotation, rayRotation, rayHit, center, box,
                    aimDir, movement, movementStrength) - 2.0f;
            candidates.add(new PointGeometry.Candidate(rayHit, score));
            bestScore = score;
        }
        for (Vec3 point : PointGeometry.projectPointsOnBox(eyes, box)) {
            if (!PointGeometry.isValidPoint(eyes, point, maxDistance, ignoreWalls)) {
                continue;
            }
            Rotation candidateRotation = Rotation.lookingAt(point, eyes).normalize();
            float score = PointGeometry.scoreCandidate(eyes, initialRotation, candidateRotation, point, center, box,
                    aimDir, movement, movementStrength);
            candidates.add(new PointGeometry.Candidate(point, score));
            if (score < bestScore) {
                bestScore = score;
            }
        }
        if (candidates.isEmpty()) {
            return null;
        }
        boolean movingTarget = movementStrength > 0.035 || Math.abs(movement.y) > 0.03;
        float threshold = movingTarget
                ? Math.max(7.0f, Math.min(28.0f, bestScore * 0.55f + 6.0f))
                : Math.max(4.0f, Math.min(18.0f, bestScore * 0.35f + 3.0f));
        List<PointGeometry.Candidate> pool = new ArrayList<>();
        for (PointGeometry.Candidate candidate : candidates) {
            if (candidate.score() <= bestScore + threshold) {
                pool.add(candidate);
            }
        }
        if (pool.isEmpty()) {
            pool = candidates;
        }
        Vec3 reference = lastSelectedPoint != null ? lastSelectedPoint : smoothedPoint;
        List<PointGeometry.Candidate> diversePool = PointGeometry.filterDiverse(pool, reference, box, movingTarget);
        if (!diversePool.isEmpty()) {
            pool = diversePool;
        }
        PointGeometry.Candidate selected = PointGeometry.selectWeighted(pool, bestScore);
        lastSelectedPoint = selected.point();
        return selected.point();
    }
    /** Lerps the freshly processed point towards the previous smoothed position so aim does not teleport. */
    private PointInsideBox applyContinuity(PointInsideBox point) {
        if (smoothedPoint == null) {
            return point;
        }
        Vec3 current = PointGeometry.clampToBox(smoothedPoint, point.box());
        Vec3 target = PointGeometry.clampToBox(point.pos(), point.box());
        double distance = current.distanceTo(target);
        if (distance <= 1.0E-4) {
            return new PointInsideBox(current, point.box());
        }
        double blend = distance > 0.35 ? 0.42 : distance > 0.15 ? 0.28 : 0.18;
        Vec3 blended = new Vec3(
                Mth.lerp(blend, current.x, target.x),
                Mth.lerp(blend, current.y, target.y),
                Mth.lerp(blend, current.z, target.z)
        );
        return new PointInsideBox(blended, point.box());
    }
}
