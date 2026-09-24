package dev.loki.lovisual.mixins.world.block;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import dev.loki.lovisual.util.player.movement.EnvironmentMovementController;

@Mixin(HoneyBlock.class)
public class HoneyBlockMixin {

    @Redirect(
            method = "doSlideMovement",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private void nostun$preserveHorizontalSlide(Entity entity, Vec3 velocity) {
        if (!EnvironmentMovementController.shouldPreserveHoneySlide(entity)) {
            entity.setDeltaMovement(velocity);
            return;
        }

        Vec3 current = entity.getDeltaMovement();
        entity.setDeltaMovement(new Vec3(current.x, velocity.y, current.z));
    }
}
