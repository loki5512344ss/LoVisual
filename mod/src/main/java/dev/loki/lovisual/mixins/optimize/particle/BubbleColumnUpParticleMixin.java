package dev.loki.lovisual.mixins.optimize.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.lovisual.features.module.modules.misc.optimize.particle.BubbleColumnCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.BubbleColumnUpParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stage 3: bubble-column fluid cache. {@code BubbleColumnUpParticle.tick}
 * calls {@code level.getFluidState(pos)} once per particle per tick — but
 * thousands of bubble particles share a few columns, so each block's fluid
 * is looked up thousands of times. This mixin wraps the call to consult
 * {@link BubbleColumnCache} first; on a miss, the original runs and the
 * result is stored.
 *
 * The cache logic is in {@link BubbleColumnCache} so the mixin stays a thin
 * wrapper and the cache is unit-testable without Minecraft types.
 *
 * <p>Gated on the {@code microOpts} toggle (Stage 1). When the toggle is off,
 * the original call runs unchanged.
 */
@Mixin(BubbleColumnUpParticle.class)
public abstract class BubbleColumnUpParticleMixin {

    @WrapOperation(
            method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientLevel;getFluidState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/material/FluidState;")
    )
    private FluidState lovisual$cachedFluid(ClientLevel level, BlockPos pos, Operation<FluidState> original) {
        if (!BubbleColumnCache.enabled()) {
            return original.call(level, pos);
        }
        long tick = level.getGameTime();
        long key = pos.asLong();
        FluidState fluid = BubbleColumnCache.get(key, tick);
        if (fluid == null) {
            fluid = original.call(level, pos);
            BubbleColumnCache.put(key, fluid);
        }
        return fluid;
    }
}
