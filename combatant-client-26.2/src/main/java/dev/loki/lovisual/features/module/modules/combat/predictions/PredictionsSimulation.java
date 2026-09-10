/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.combat.predictions;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.EntityCollision;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.Kind;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel.TrajectoryResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import dev.loki.lovisual.features.module.modules.combat.prediction.Predictions;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsGeometry;
import dev.loki.lovisual.features.module.modules.combat.predictions.PredictionsModel;

/**
 * Tick-by-tick projectile trajectory simulation and entity collision resolution
 * for the Predictions module.
 */
public final class PredictionsSimulation {
    private PredictionsSimulation() {
    }

    public static final int HIT_SELF_COLOR = 0xFFFF4444;
    public static final int HIT_ENTITY_COLOR = 0xFFFFCC44;

    public static TrajectoryResult simulateEntity(Entity e, Kind kind, float tickDelta,
                                                  NumberValue<Integer> maxSimTicks,
                                                  NumberValue<Integer> pearlMaxSimTicks,
                                                  BooleanValue pearlColorToggle,
                                                  RGBColorValue pearlColorValue) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel world = mc.level;
        if (world == null) return null;

        Vec3 pos = e.position();
        Vec3 vel = e.getDeltaMovement();

        List<Vec3> points = new ArrayList<>();
        points.add(pos);

        double t = 0.0;
        int baseMaxTicks = Math.max(1, maxSimTicks.get());
        int maxTicks = kind == Kind.PEARL_ENTITY
                ? Math.max(1, pearlMaxSimTicks.get())
                : baseMaxTicks;
        int hitColor = 0;
        long impactTick = world.getGameTime();
        Vec3 impactPos = null;
        Vec3 impactNormal = null;
        Entity hitEntity = null;
        boolean hitBlock = false;

        for (int i = 0; i < maxTicks; i++) {
            Vec3 segmentStart = pos;
            Vec3 simulatedVelocity = PredictionsGeometry.advanceTickVelocity(e, vel);
            Vec3 simulatedPos = pos.add(simulatedVelocity);
            double segmentLengthSq = segmentStart.distanceToSqr(simulatedPos);

            HitResult hit = world.clip(new ClipContext(
                    segmentStart,
                    simulatedPos,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    e
            ));

            EntityCollision entityCollision = getEntityCollision(e, segmentStart, simulatedPos);
            boolean hasBlockHit = hit.getType() != HitResult.Type.MISS;
            boolean useEntityHit = entityCollision != null
                    && (!hasBlockHit || segmentStart.distanceToSqr(entityCollision.hitPos()) <= segmentStart.distanceToSqr(hit.getLocation()));

            if (useEntityHit) {
                hitColor = (entityCollision.entity() == mc.player) ? HIT_SELF_COLOR : HIT_ENTITY_COLOR;
                Vec3 entityHitPos = entityCollision.hitPos();
                t += PredictionsGeometry.partialStepProgress(segmentStart, entityHitPos, segmentLengthSq);
                points.add(entityHitPos);
                impactTick = world.getGameTime() + (long) Math.ceil(t);
                impactPos = entityHitPos;
                impactNormal = entityCollision.hitNormal();
                hitEntity = entityCollision.entity();
                break;
            } else if (hasBlockHit) {
                Vec3 blockHitPos = hit.getLocation();
                t += PredictionsGeometry.partialStepProgress(segmentStart, blockHitPos, segmentLengthSq);
                points.add(blockHitPos);
                impactTick = world.getGameTime() + (long) Math.ceil(t);
                impactPos = blockHitPos;
                impactNormal = PredictionsGeometry.resolveImpactNormal(hit, simulatedVelocity);
                hitBlock = true;
                break;
            }

            pos = simulatedPos;
            vel = simulatedVelocity;
            t += 1.0;
            points.add(pos);
            impactTick = world.getGameTime() + (long) Math.ceil(t);
        }

        ItemStack icon = switch (kind) {
            case PEARL_ENTITY -> new ItemStack(Items.ENDER_PEARL);
            case TRIDENT -> new ItemStack(Items.TRIDENT);
            case FIREBALL -> new ItemStack(Items.FIRE_CHARGE);
            case WIND_CHARGE -> new ItemStack(Items.WIND_CHARGE);
            case FIREWORK -> new ItemStack(Items.FIREWORK_ROCKET);
            case SNOWBALL -> new ItemStack(Items.SNOWBALL);
            case EGG -> new ItemStack(Items.EGG);
            case POTION_SPLASH -> new ItemStack(Items.SPLASH_POTION);
            case POTION_LINGERING -> new ItemStack(Items.LINGERING_POTION);
            default -> new ItemStack(Items.ARROW);
        };

        int color = hitColor;
        if (color == 0 && kind == Kind.PEARL_ENTITY && pearlColorToggle.get()) {
            color = pearlColorValue.getArgb();
        }

        return new TrajectoryResult(points, t, impactTick, icon, color, kind, impactPos, impactNormal, hitEntity, hitBlock);
    }

    private static EntityCollision getEntityCollision(Entity movingEntity, Vec3 startPos, Vec3 endPos) {
        Vec3 direction = endPos.subtract(startPos);
        if (direction.lengthSqr() == 0.0) {
            return null;
        }

        AABB projectileBox = movingEntity.getBoundingBox().move(startPos.subtract(movingEntity.position()));
        AABB collisionBox = projectileBox.expandTowards(direction).inflate(0.5);
        EntityHitResult hitResult = ProjectileUtil.getEntityHitResult(
                movingEntity,
                startPos,
                endPos,
                collisionBox,
                entity -> canHitPredictedEntity(movingEntity, entity, collisionBox)
                        && entity instanceof LivingEntity
                        && !(entity instanceof ItemEntity)
                        && !(entity instanceof ExperienceOrb)
                        && entity != movingEntity,
                direction.lengthSqr()
        );
        if (hitResult == null) {
            return null;
        }

        return new EntityCollision(
                hitResult.getEntity(),
                hitResult.getLocation(),
                PredictionsModel.resolveEntityImpactNormal(hitResult.getEntity().getBoundingBox(), hitResult.getLocation(), direction)
        );
    }

    private static boolean canHitPredictedEntity(Entity projectile, Entity entity, AABB collisionBox) {
        if (projectile == null || entity == null || !entity.isAlive() || !entity.canBeHitByProjectile()) {
            return false;
        }

        if (projectile instanceof Projectile projectileEntity) {
            Entity owner = projectileEntity.getOwner();
            if (owner != null && !hasPredictedLeftOwner(owner, entity, collisionBox)) {
                return !owner.isPassengerOfSameVehicle(entity);
            }
        }

        return true;
    }

    private static boolean hasPredictedLeftOwner(Entity owner, Entity entity, AABB collisionBox) {
        if (owner == null || collisionBox == null) {
            return true;
        }
        return owner.getRootVehicle()
                .getSelfAndPassengers()
                .filter(Entity::isPickable)
                .noneMatch(passenger -> collisionBox.intersects(passenger.getBoundingBox()));
    }
}
