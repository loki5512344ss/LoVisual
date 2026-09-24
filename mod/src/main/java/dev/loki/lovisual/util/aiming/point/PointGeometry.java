package dev.loki.lovisual.util.aiming.point;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.raycast.RaycastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Pure geometry, scoring and small random helpers shared by {@link PointTracker}
 * and {@link PointProcessor}. Package-private: not part of the public aiming API.
 */
final class PointGeometry {
    private PointGeometry() {}
    /** Composite cost used by {@link PointTracker#findPoint} to rank candidate aim points. */
    static float scoreCandidate(Vec3 eyes, Rotation initialRotation, Rotation candidateRotation,
                                Vec3 point, Vec3 center, AABB box, Vec3 aimDir,
                                Vec3 movement, double movementStrength) {
        float rotationScore = initialRotation.angleTo(candidateRotation) * 1.15f;
        double halfX = Math.max(1.0E-4, box.getXsize() * 0.5);
        double halfY = Math.max(1.0E-4, box.getYsize() * 0.5);
        double halfZ = Math.max(1.0E-4, box.getZsize() * 0.5);
        double nx = Math.abs(point.x - center.x) / halfX;
        double ny = Math.abs(point.y - center.y) / halfY;
        double nz = Math.abs(point.z - center.z) / halfZ;
        double edgeFactor = Math.max(Math.max(nx, nz), ny * 0.65);
        double centerPenalty = (1.0 - Mth.clamp(edgeFactor, 0.0, 1.0)) * (movementStrength > 0.035 ? 6.0 : 4.0);
        double rayDistance = distanceToRay(eyes, aimDir, point);
        double rayScale = Math.max(0.08, Math.max(box.getXsize(), Math.max(box.getYsize(), box.getZsize())) * 0.5);
        double rayScore = (rayDistance / rayScale) * (movementStrength > 0.035 ? 5.5 : 4.0);
        double movementBenefit = movingPointBenefit(point, center, box, movement, movementStrength);
        return rotationScore + (float) rayScore + (float) centerPenalty - (float) movementBenefit;
    }
    /** Drops candidates that sit too close to {@code reference}, encouraging aim diversity. */
    static List<Candidate> filterDiverse(List<Candidate> candidates, Vec3 reference, AABB box, boolean movingTarget) {
        List<Candidate> out = new ArrayList<>();
        if (reference == null) return out;
        double minAxis = Math.min(box.getXsize(), Math.min(box.getYsize(), box.getZsize()));
        double minDistance = Math.max(movingTarget ? 0.085 : 0.055, minAxis * (movingTarget ? 0.26 : 0.18));
        double minDistanceSq = minDistance * minDistance;
        for (Candidate candidate : candidates) {
            if (candidate.point().distanceToSqr(reference) >= minDistanceSq) out.add(candidate);
        }
        return out;
    }
    /** Softmax-like weighted pick among close-scoring candidates to avoid deterministic aim. */
    static Candidate selectWeighted(List<Candidate> candidates, float bestScore) {
        if (candidates.size() == 1) return candidates.get(0);
        double total = 0.0;
        double[] weights = new double[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            double delta = Math.max(0.0, candidates.get(i).score() - bestScore);
            double weight = 1.0 / (1.0 + delta * delta * 0.08);
            weights[i] = weight;
            total += weight;
        }
        double pick = ThreadLocalRandom.current().nextDouble(total);
        for (int i = 0; i < candidates.size(); i++) {
            pick -= weights[i];
            if (pick <= 0.0) return candidates.get(i);
        }
        return candidates.get(candidates.size() - 1);
    }
    /** Bonus for leading a moving target: reward points on the forward / lateral sides of the box. */
    static double movingPointBenefit(Vec3 point, Vec3 center, AABB box, Vec3 movement, double movementStrength) {
        if (movementStrength <= 0.035) return 0.0;
        double halfX = Math.max(1.0E-4, box.getXsize() * 0.5);
        double halfZ = Math.max(1.0E-4, box.getZsize() * 0.5);
        double dirX = movement.x / movementStrength;
        double dirZ = movement.z / movementStrength;
        double offX = Mth.clamp((point.x - center.x) / halfX, -1.0, 1.0);
        double offZ = Mth.clamp((point.z - center.z) / halfZ, -1.0, 1.0);
        double lead = Mth.clamp(offX * dirX + offZ * dirZ, -1.0, 1.0);
        double sideStrength = Math.min(1.0, Math.hypot(offX, offZ));
        return Math.max(0.0, lead) * 4.0 + sideStrength * 1.4;
    }
    static double distanceToRay(Vec3 origin, Vec3 direction, Vec3 point) {
        Vec3 diff = point.subtract(origin);
        double t = Math.max(0.0, diff.dot(direction));
        return point.distanceTo(origin.add(direction.scale(t)));
    }
    /** Intersection of the aim ray with the target box, or {@code null} if the ray misses within {@code maxDistance}. */
    static Vec3 findRayHitPoint(Vec3 eyes, Vec3 aimDir, AABB box, double maxDistance) {
        double range = maxDistance > 0.0 && maxDistance < Double.MAX_VALUE ? maxDistance : 6.0;
        return box.clip(eyes, eyes.add(aimDir.scale(range))).orElse(null);
    }
    static double horizontalLength(Vec3 vec) {
        return vec == null ? 0.0 : Math.hypot(vec.x, vec.z);
    }
    static boolean isValidPoint(Vec3 startPoint, Vec3 endPoint, double maxDistance, boolean ignoreWalls) {
        if (startPoint == null || endPoint == null) return false;
        if (maxDistance > 0.0 && startPoint.distanceToSqr(endPoint) > maxDistance * maxDistance) return false;
        return ignoreWalls || RaycastUtil.hasLineOfSightPoint(startPoint, endPoint);
    }
    /** Sample a directional offset biased along the entity's motion (or perpendicular to aim when stationary). */
    static Vec3 randomInstabilityOffset(AABB box, Vec3 entityVelocity, Vec3 aimDirection, double intensity) {
        double halfX = Math.max(0.001, box.getXsize() * 0.5);
        double halfY = Math.max(0.001, box.getYsize() * 0.5);
        double halfZ = Math.max(0.001, box.getZsize() * 0.5);
        double speed = horizontalLength(entityVelocity);
        double sideX, sideZ;
        if (speed > 0.035) {
            sideX = entityVelocity.x / speed;
            sideZ = entityVelocity.z / speed;
        } else {
            Vec3 horizontalAim = new Vec3(aimDirection.x, 0.0, aimDirection.z);
            double aimLen = Math.max(1.0E-4, horizontalLength(horizontalAim));
            sideX = -horizontalAim.z / aimLen;
            sideZ = horizontalAim.x / aimLen;
        }
        double lateral = randomDouble(-1.0, 1.0) * (0.14 + 0.22 * intensity);
        double lead = speed > 0.035 ? randomDouble(0.05, 0.24) * intensity : 0.0;
        double vertical = randomDouble(-0.16, 0.14) * intensity;
        return new Vec3(
                sideX * halfX * lateral + sideX * halfX * lead,
                halfY * vertical,
                sideZ * halfZ * lateral + sideZ * halfZ * lead
        );
    }
    static Vec3 clampToBox(Vec3 point, AABB box) {
        return new Vec3(
                Mth.clamp(point.x, box.minX, box.maxX),
                Mth.clamp(point.y, box.minY, box.maxY),
                Mth.clamp(point.z, box.minZ, box.maxZ)
        );
    }
    static Vec3 getNearestPoint(AABB box, Vec3 eyes) {
        return new Vec3(
                Mth.clamp(eyes.x, box.minX, box.maxX),
                Mth.clamp(eyes.y, box.minY, box.maxY),
                Mth.clamp(eyes.z, box.minZ, box.maxZ)
        );
    }
    /** Sample a grid of surface (or interior, when eyes are inside) points on the target's bounding box. */
    static List<Vec3> projectPointsOnBox(Vec3 eyes, AABB box) {
        List<Vec3> points = new ArrayList<>();
        double[] steps = proportions();
        if (!box.contains(eyes)) {
            for (double y : steps) {
                for (double z : steps) {
                    points.add(new Vec3(box.minX, box.minY + box.getYsize() * y, box.minZ + box.getZsize() * z));
                    points.add(new Vec3(box.maxX, box.minY + box.getYsize() * y, box.minZ + box.getZsize() * z));
                }
            }
            for (double x : steps) {
                for (double z : steps) {
                    points.add(new Vec3(box.minX + box.getXsize() * x, box.minY, box.minZ + box.getZsize() * z));
                    points.add(new Vec3(box.minX + box.getXsize() * x, box.maxY, box.minZ + box.getZsize() * z));
                }
            }
            for (double x : steps) {
                for (double y : steps) {
                    points.add(new Vec3(box.minX + box.getXsize() * x, box.minY + box.getYsize() * y, box.minZ));
                    points.add(new Vec3(box.minX + box.getXsize() * x, box.minY + box.getYsize() * y, box.maxZ));
                }
            }
            return points;
        }
        for (double x : steps) {
            for (double y : steps) {
                for (double z : steps) {
                    points.add(new Vec3(
                            box.minX + box.getXsize() * x,
                            box.minY + box.getYsize() * y,
                            box.minZ + box.getZsize() * z
                    ));
                }
            }
        }
        return points;
    }
    static int randomInt(int min, int max) {
        if (max <= min) return min;
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }
    static double randomDouble(double min, double max) {
        if (max <= min) return min;
        return ThreadLocalRandom.current().nextDouble(min, max);
    }
    private static double[] proportions() {
        double[] steps = new double[13];
        for (int i = 0; i < steps.length; i++) {
            steps[i] = (i + 0.5) / steps.length;
        }
        return steps;
    }
    /** Scored aim candidate used by {@link PointTracker} during multi-point selection. */
    record Candidate(Vec3 point, float score) {}
}
