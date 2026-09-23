/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.projectile.calc;

import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.projectile.calc.CydhranianProjectileAngleCalculator;
import dev.loki.lovisual.util.projectile.calc.PolynomialProjectileAngleCalculator;
import dev.loki.lovisual.util.projectile.calc.ProjectileAngleCalculator;
import dev.loki.lovisual.util.projectile.targeting.ProjectileTarget;
import dev.loki.lovisual.util.projectile.trajectory.TrajectoryInfo;

public final class SituationalProjectileAngleCalculator extends ProjectileAngleCalculator {

    public static final SituationalProjectileAngleCalculator INSTANCE = new SituationalProjectileAngleCalculator();

    private SituationalProjectileAngleCalculator() {
    }

    @Override
    public Rotation calculateAngleFor(TrajectoryInfo projectileInfo, Vec3 sourcePos, ProjectileTarget target) {
        Vec3 basePos = target.getPositionInTicks(0.0);
        ProjectileAngleCalculator implementation = basePos.distanceToSqr(sourcePos) < 25.0
                ? PolynomialProjectileAngleCalculator.INSTANCE
                : CydhranianProjectileAngleCalculator.INSTANCE;
        return implementation.calculateAngleFor(projectileInfo, sourcePos, target);
    }
}
