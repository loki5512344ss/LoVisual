package dev.loki.lovisual.mixins.entity.world;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@Mixin(ProjectileUtil.class)
public class ProjectileUtilMixin {

    // Intercept getOtherEntities in ProjectileUtil.raycast:
    // - If Hitbox is disabled -> keep vanilla list.
    // - If enabled -> drop dead LivingEntity and those Hitbox wants to ignore.
    @Redirect(
            method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;"
            )
    )
    private static List<Entity> lovisual$filterEntities(Level world, Entity source, AABB box, Predicate<Entity> predicate) {
        // Hitbox module removed; keep vanilla entity list.
        return world.getEntities(source, box, predicate);
    }

    // Intercept getBoundingBox: add padding only for live targets allowed by Hitbox.
    @Redirect(
            method = "getEntityHitResult(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;D)Lnet/minecraft/world/phys/EntityHitResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"
            )
    )
    private static AABB lovisual$padBoundingBox(Entity target,
                                                 Entity source,
                                                 Vec3 start,
                                                 Vec3 end,
                                                 AABB box,
                                                 Predicate<Entity> predicate,
                                                 double maxDistance) {
        // Hitbox module removed; keep vanilla bounding box.
        return target.getBoundingBox();
    }
}
