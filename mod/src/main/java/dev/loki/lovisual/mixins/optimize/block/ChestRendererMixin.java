package dev.loki.lovisual.mixins.optimize.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.lovisual.features.module.modules.misc.optimize.block.CachedLight;
import dev.loki.lovisual.features.module.modules.misc.optimize.block.TickCached;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stage 3: double-chest combine cache + double-chest pair-light cache.
 *
 * <p>Two WrapOperations on {@link ChestRenderer#extractRenderState}:
 * <ul>
 *   <li><b>Combine cache</b> — wraps {@link ChestBlock#combine} (the
 *       neighbor-lookup that builds the double-chest render state). The
 *       result only changes when the neighbour block changes, so caching by
 *       game tick (via the {@link TickCached} mixin from Stage 3-continued-2)
 *       removes the neighbor-walk from the per-frame hot path.</li>
 *   <li><b>Pair-light cache</b> — wraps {@link Int2IntFunction#applyAsInt}
 *       (the second light lookup done for double chests). Two light lookups
 *       per chest per frame in vanilla, cached to one per tick via the
 *       {@link CachedLight#lovisual$pairLight()} slot.</li>
 * </ul>
 *
 * Combine cache gated on {@code blockEntityCache} toggle; pair-light cache
 * gated on {@code microOpts} toggle.
 */
@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin {

    @WrapOperation(
            method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/ChestBlock;combine(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/level/block/DoubleBlockCombiner$NeighborCombineResult;")
    )
    private DoubleBlockCombiner.NeighborCombineResult<?> lovisual$cachedCombine(
            ChestBlock block, BlockState state, Level level, BlockPos pos, boolean ignoreBlocked,
            Operation<DoubleBlockCombiner.NeighborCombineResult<?>> original,
            BlockEntity blockEntity, ChestRenderState renderState, float partialTick, Vec3 camera,
            ModelFeatureRenderer.CrumblingOverlay crumbling) {
        if (!OptimizeToggles.blockEntityCache()) {
            return original.call(block, state, level, pos, ignoreBlocked);
        }
        if (!(blockEntity instanceof TickCached cache)) {
            return original.call(block, state, level, pos, ignoreBlocked);
        }
        long tick = level.getGameTime();
        if (cache.lovisual$cachedTick() != tick || cache.lovisual$cached() == null) {
            cache.lovisual$setCached(original.call(block, state, level, pos, ignoreBlocked), tick);
        }
        return (DoubleBlockCombiner.NeighborCombineResult<?>) cache.lovisual$cached();
    }

    @WrapOperation(
            method = "extractRenderState(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState;FLnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
            at = @At(value = "INVOKE",
                    target = "Lit/unimi/dsi/fastutil/ints/Int2IntFunction;applyAsInt(I)I")
    )
    private int lovisual$cachedPairLight(Int2IntFunction combiner, int light, Operation<Integer> original,
                                        BlockEntity blockEntity, ChestRenderState renderState, float partialTick, Vec3 camera,
                                        ModelFeatureRenderer.CrumblingOverlay crumbling) {
        Level level = blockEntity.getLevel();
        if (!OptimizeToggles.microOpts() || level == null) {
            return original.call(combiner, light);
        }
        if (!(blockEntity instanceof CachedLight cache)) {
            return original.call(combiner, light);
        }
        long tick = level.getGameTime();
        if (cache.lovisual$pairLightTick() != tick) {
            cache.lovisual$setPairLight(original.call(combiner, light), tick);
        }
        return cache.lovisual$pairLight();
    }
}
