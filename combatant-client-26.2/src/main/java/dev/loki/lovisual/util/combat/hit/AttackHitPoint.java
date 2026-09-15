/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.combat.hit;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Точная точка удара по хитбоксу цели — трасса луча от камеры вдоль последнего
 * известного взгляда игрока. Общая для эффектов, которым важна не позиция цели
 * целиком, а конкретное место попадания (HitEffect, HitBubbles).
 */
public enum AttackHitPoint {
    ;

    /** @return точка попадания на хитбоксе {@code target} или null, если трасса не пересекла его. */
    public static Vec3 resolve(Entity target, float lastYaw, float lastPitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return null;

        double distance = mc.player.distanceTo(target) + 1.0;
        Vec3 start = mc.player.position().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        Vec3 rot = rotationVector(lastPitch, lastYaw);
        Vec3 end = start.add(rot.scale(distance));
        AABB box = mc.player.getBoundingBox().expandTowards(rot.scale(distance)).inflate(1.0, 1.0, 1.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
                mc.player,
                start,
                end,
                box,
                e -> !e.isSpectator() && e.isPickable() && e == target,
                distance * distance
        );

        return hit != null ? hit.getLocation() : null;
    }

    private static Vec3 rotationVector(float pitch, float yaw) {
        float yawRad = -yaw * ((float) Math.PI / 180F);
        float pitchRad = pitch * ((float) Math.PI / 180F);
        float cosYaw = Mth.cos(yawRad);
        float sinYaw = Mth.sin(yawRad);
        float cosPitch = Mth.cos(pitchRad);
        float sinPitch = Mth.sin(pitchRad);
        return new Vec3(sinYaw * cosPitch, -sinPitch, cosYaw * cosPitch);
    }
}
