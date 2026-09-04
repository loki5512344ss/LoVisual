/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.projectile;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.data.Rotation;

public abstract class ProjectileAngleCalculator {

    public abstract Rotation calculateAngleFor(TrajectoryInfo projectileInfo, Vec3 sourcePos, ProjectileTarget target);

    public Rotation calculateAngleForStaticTarget(TrajectoryInfo projectileInfo, Vec3 targetPos, AABB targetBox, Vec3 sourcePos) {
        return calculateAngleFor(projectileInfo, sourcePos, ProjectileTarget.constant(targetPos, targetBox));
    }

    public Rotation calculateAngleForEntity(TrajectoryInfo projectileInfo, LivingEntity entity, Vec3 sourcePos) {
        return calculateAngleFor(projectileInfo, sourcePos, ProjectileTarget.forEntity(entity));
    }

    public Rotation calculateAngleForEntity(TrajectoryInfo projectileInfo, LivingEntity entity) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player == null ? null : calculateAngleForEntity(projectileInfo, entity, mc.player.getEyePosition());
    }
}
