/*
 * This file is part of the LoVisual Client distribution.
 * LoVisual modifications copyright (c) 2026 pivosos2007.
 *
 * Portions of this file are based on LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce).
 * Copyright (c) 2015-2026 CCBlueX.
 *
 * LiquidBounce portions are licensed under GPLv3-or-later.
 * LoVisual modifications are licensed under GPLv3.
 * See THIRD_PARTY_NOTICES.md for details.
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
