package dev.loki.lovisual.mixins.world.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SlimeBlock.class)
public class SlimeBlockMixin {

    @Inject(method = "stepOn", at = @At("HEAD"), cancellable = true)
    private void nostun$onSteppedOn(Level world, BlockPos pos, BlockState state, Entity entity, CallbackInfo ci) {
        // NoStun module removed; no slime-block override remains.
    }
}
