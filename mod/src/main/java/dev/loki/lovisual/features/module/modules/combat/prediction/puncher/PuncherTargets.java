package dev.loki.lovisual.features.module.modules.combat.prediction;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import dev.loki.lovisual.util.target.TargetingUtil;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class PuncherTargets {
    static final String PROJECTILE_FIREBALL = "fireball";
    static final String PROJECTILE_SHULKER_BULLET = "shulker_bullet";
    private final ProjectilePuncher module;

    PuncherTargets(ProjectilePuncher module) {
        this.module = module;
    }

    static Map<String, Boolean> projectileDefaults() {
        return Map.of(PROJECTILE_FIREBALL, true, PROJECTILE_SHULKER_BULLET, true);
    }

    List<Entity> findCandidates() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return List.of();
        double searchRange = Math.max(module.range.get(), module.wallRange.get()) + 8.0;
        List<Entity> candidates = mc.level.getEntities(mc.player, mc.player.getBoundingBox().inflate(searchRange), this::isSupportedProjectile);
        candidates.removeIf(e -> !shouldAttack(e));
        candidates.sort(Comparator.comparingDouble(this::predictedDistanceSq));
        return candidates;
    }

    boolean shouldAttack(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        Vec3 velocity = entity.getDeltaMovement();
        if (velocity.lengthSqr() < 1.0E-7) return false;
        Vec3 entityPos = entity.position();
        Vec3 vecToPlayer = mc.player.getBoundingBox().getCenter().subtract(entityPos);
        double dot = vecToPlayer.dot(velocity);
        boolean movingTowardsPlayer = dot > 0.5 * velocity.length() * vecToPlayer.length();
        AABB extendedHitbox = mc.player.getBoundingBox().inflate(entity.getBoundingBox().getXsize() * 0.5);
        boolean touchesHitbox = extendedHitbox.clip(entityPos, entityPos.add(velocity.scale(20.0))).isPresent();
        boolean willHitPlayer = !extendedHitbox.contains(entityPos) && touchesHitbox;
        if (!movingTowardsPlayer && !willHitPlayer) return false;
        return predictedDistanceSq(entity) <= module.range.get() * module.range.get();
    }

    boolean isSupportedProjectile(Entity entity) {
        if (entity == null || entity.isRemoved()) return false;
        EntityType<?> type = entity.getType();
        if (type == net.minecraft.world.entity.EntityTypes.FIREBALL) return module.projectiles.get(PROJECTILE_FIREBALL);
        if (type == net.minecraft.world.entity.EntityTypes.SHULKER_BULLET) return module.projectiles.get(PROJECTILE_SHULKER_BULLET);
        return false;
    }

    AABB predictBox(Entity entity) {
        int ticks = Math.max(0, module.predictTicks.get());
        if (ticks <= 0) return entity.getBoundingBox();
        return entity.getBoundingBox().move(entity.getDeltaMovement().scale(ticks));
    }

    double predictedDistanceSq(Entity entity) {
        Minecraft mc = Minecraft.getInstance();
        return TargetingUtil.distanceToBoxSq(mc.player.getEyePosition(), predictBox(entity));
    }
}
