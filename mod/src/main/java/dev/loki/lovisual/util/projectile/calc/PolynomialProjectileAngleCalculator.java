package dev.loki.lovisual.util.projectile.calc;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.projectile.calc.ProjectileAngleCalculator;
import dev.loki.lovisual.util.projectile.targeting.ProjectileTarget;
import dev.loki.lovisual.util.projectile.trajectory.TrajectoryInfo;

public final class PolynomialProjectileAngleCalculator extends ProjectileAngleCalculator {

    public static final PolynomialProjectileAngleCalculator INSTANCE = new PolynomialProjectileAngleCalculator();

    private PolynomialProjectileAngleCalculator() {
    }

    @Override
    public Rotation calculateAngleFor(TrajectoryInfo projectileInfo, Vec3 sourcePos, ProjectileTarget target) {
        Vec3 basePos = target.getPositionInTicks(0.0);
        double estimatedTicksUntilImpact = basePos.distanceTo(sourcePos) / projectileInfo.initialVelocity();
        Vec3 diff = target.getPositionInTicks(estimatedTicksUntilImpact).subtract(sourcePos);

        double horizontalDistance = Math.hypot(diff.x, diff.z);
        double velocity = projectileInfo.initialVelocity();
        double gravity = projectileInfo.gravity();
        double velocity2 = velocity * velocity;
        double velocity4 = velocity2 * velocity2;
        double y = diff.y;

        double sqrtValue = velocity4 - gravity * (gravity * horizontalDistance * horizontalDistance + 2.0 * y * velocity2);
        if (sqrtValue < 0.0) {
            return null;
        }

        double pitchRad = Math.atan((velocity2 - Math.sqrt(sqrtValue)) / (gravity * horizontalDistance));
        double yawRad = Math.atan2(diff.z, diff.x);

        return new Rotation(
                Mth.wrapDegrees((float) Math.toDegrees(yawRad) - 90.0f),
                Mth.wrapDegrees((float) -Math.toDegrees(pitchRad))
        );
    }
}
