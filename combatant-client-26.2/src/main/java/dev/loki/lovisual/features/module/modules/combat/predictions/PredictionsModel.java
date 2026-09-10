/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.combat.predictions;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.hurtingprojectile.Fireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.WindCharge;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.entity.projectile.throwableitemprojectile.AbstractThrownPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import dev.loki.lovisual.features.module.modules.combat.prediction.Predictions;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsGeometry;

/**
 * Shared value types and item/entity classification helpers for the Predictions module.
 */
public final class PredictionsModel {
    private PredictionsModel() {
    }

    public enum Kind {
        PEARL_ENTITY,
        ARROW,
        TRIDENT,
        FIREBALL,
        WIND_CHARGE,
        FIREWORK,
        SNOWBALL,
        EGG,
        POTION_SPLASH,
        POTION_LINGERING
    }

    public record TrajectoryResult(List<Vec3> points, double flightTicks, long impactTick, ItemStack iconStack,
                                   int argb, Kind kind, Vec3 impactPos, Vec3 impactNormal, Entity hitEntity,
                                   boolean hitBlock) {
        public TrajectoryResult(List<Vec3> points, double flightTicks, long impactTick, ItemStack iconStack,
                                int argb, Kind kind, Vec3 impactPos, Vec3 impactNormal, Entity hitEntity, boolean hitBlock) {
            this.points = points;
            this.flightTicks = flightTicks;
            this.impactTick = impactTick;
            this.iconStack = iconStack == null ? ItemStack.EMPTY : iconStack;
            this.argb = argb;
            this.kind = kind;
            this.impactPos = impactPos;
            this.impactNormal = impactNormal;
            this.hitEntity = hitEntity;
            this.hitBlock = hitBlock;
        }
    }

    public record AimPrediction(Entity entity, Vec3 hitPos, Vec3 anchorPos, Vec3 hitNormal, ItemStack displayStack,
                                TrajectoryResult trajectory) {
        public AimPrediction(Entity entity, Vec3 hitPos, Vec3 anchorPos, Vec3 hitNormal, ItemStack displayStack,
                             TrajectoryResult trajectory) {
            this.entity = entity;
            this.hitPos = hitPos;
            this.anchorPos = anchorPos;
            this.hitNormal = hitNormal;
            this.displayStack = displayStack == null ? ItemStack.EMPTY : displayStack;
            this.trajectory = trajectory;
        }
    }

    public record EntityCollision(Entity entity, Vec3 hitPos, Vec3 hitNormal) {
    }

    public record TimerPlate(float textX, float textY, String text,
                             float iconX, float iconY, float iconScale, ItemStack iconStack) {
        public TimerPlate(float textX, float textY, String text,
                          float iconX, float iconY, float iconScale, ItemStack iconStack) {
            this.textX = textX;
            this.textY = textY;
            this.text = text;
            this.iconX = iconX;
            this.iconY = iconY;
            this.iconScale = iconScale;
            this.iconStack = iconStack == null ? ItemStack.EMPTY : iconStack;
        }
    }

    public static Kind kindFromParams(ItemStack stack) {
        if (stack == null) return Kind.ARROW;
        if (stack.is(Items.TRIDENT)) return Kind.TRIDENT;
        if (stack.is(Items.SPLASH_POTION)) return Kind.POTION_SPLASH;
        if (stack.is(Items.LINGERING_POTION)) return Kind.POTION_LINGERING;
        if (stack.is(Items.SNOWBALL)) return Kind.SNOWBALL;
        if (stack.is(Items.EGG)) return Kind.EGG;
        if (stack.is(Items.FIREWORK_ROCKET)) return Kind.FIREWORK;
        if (stack.is(Items.WIND_CHARGE)) return Kind.WIND_CHARGE;
        if (stack.is(Items.FIRE_CHARGE)) return Kind.FIREBALL;
        if (stack.is(Items.ENDER_PEARL)) return Kind.PEARL_ENTITY;
        return Kind.ARROW;
    }

    public static Kind kindFromProjectile(Entity entity) {
        if (entity instanceof ThrownTrident) return Kind.TRIDENT;
        if (entity instanceof AbstractThrownPotion potion) {
            return potion.getItem().is(Items.LINGERING_POTION) ? Kind.POTION_LINGERING : Kind.POTION_SPLASH;
        }
        if (entity instanceof Snowball) return Kind.SNOWBALL;
        if (entity instanceof ThrownEgg) return Kind.EGG;
        if (entity instanceof FireworkRocketEntity) return Kind.FIREWORK;
        if (entity instanceof WindCharge) return Kind.WIND_CHARGE;
        if (entity instanceof Fireball) return Kind.FIREBALL;
        if (entity instanceof ThrownEnderpearl) return Kind.PEARL_ENTITY;
        return Kind.ARROW;
    }

    public static Vec3 resolveEntityImpactNormal(AABB box, Vec3 hitPos, Vec3 fallbackVelocity) {
        if (box == null || hitPos == null) {
            return PredictionsGeometry.resolveImpactNormal(null, fallbackVelocity);
        }

        double dxMin = Math.abs(hitPos.x - box.minX);
        double dxMax = Math.abs(box.maxX - hitPos.x);
        double dyMin = Math.abs(hitPos.y - box.minY);
        double dyMax = Math.abs(box.maxY - hitPos.y);
        double dzMin = Math.abs(hitPos.z - box.minZ);
        double dzMax = Math.abs(box.maxZ - hitPos.z);

        double best = dxMin;
        Vec3 normal = new Vec3(-1.0, 0.0, 0.0);
        if (dxMax < best) {
            best = dxMax;
            normal = new Vec3(1.0, 0.0, 0.0);
        }
        if (dyMin < best) {
            best = dyMin;
            normal = new Vec3(0.0, -1.0, 0.0);
        }
        if (dyMax < best) {
            best = dyMax;
            normal = new Vec3(0.0, 1.0, 0.0);
        }
        if (dzMin < best) {
            best = dzMin;
            normal = new Vec3(0.0, 0.0, -1.0);
        }
        if (dzMax < best) {
            normal = new Vec3(0.0, 0.0, 1.0);
        }
        return normal;
    }
}
