package dev.loki.lovisual.mixins.world.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static net.minecraft.world.level.material.FlowingFluid.FALLING;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {

    @Shadow
    protected abstract boolean isSolidFace(BlockGetter world, BlockPos pos, Direction direction);

    @Inject(method = "getFlow", at = @At("HEAD"), cancellable = true)
    private void lovisual$getVelocity(BlockGetter world, BlockPos pos, FluidState state, CallbackInfoReturnable<Vec3> cir) {
        // NoPush module removed; no fluid velocity override remains.
    }
}
