package dev.loki.lovisual.mixins.world.block;

import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import dev.loki.lovisual.util.player.movement.EnvironmentMovementController;

@Mixin(Block.class)
public abstract class BlockMixin {

    @Inject(
            method = "getFriction",
            at = @At("HEAD"),
            cancellable = true
    )
    private void nostun$overrideSlipperiness(CallbackInfoReturnable<Float> cir) {
        Float override = EnvironmentMovementController.getSlipperinessOverride((Block) (Object) this);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }

    @Inject(
            method = "getSpeedFactor",
            at = @At("HEAD"),
            cancellable = true
    )
    private void nostun$overrideVelocityMultiplier(CallbackInfoReturnable<Float> cir) {
        Float override = EnvironmentMovementController.getVelocityMultiplierOverride((Block) (Object) this);
        if (override != null) {
            cir.setReturnValue(override);
        }
    }
}
