package dev.loki.lovisual.util.projectile.calc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.aiming.data.Rotation;
import dev.loki.lovisual.util.projectile.targeting.ProjectileTarget;
import dev.loki.lovisual.util.projectile.trajectory.TrajectoryInfo;

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
