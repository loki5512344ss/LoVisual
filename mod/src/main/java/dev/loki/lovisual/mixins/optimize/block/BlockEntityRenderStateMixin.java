package dev.loki.lovisual.mixins.optimize.block;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.loki.lovisual.features.module.modules.misc.optimize.block.CachedLight;
import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.BlockAndLightGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Stage 3: cache per-tick block-light lookups on every block-entity in view.
 *
 * <p>Vanilla {@code BlockEntityRenderState.extractBase} calls
 * {@link LightCoordsUtil#getLightCoords} every frame per block-entity — that
 * walks the chunk's light section data per call, and the light value only
 * changes on tick boundaries. This WrapOperation wraps the call to consult
 * the per-tick cache slot on the {@link BlockEntity} (via the
 * {@link CachedLight} mixin from Stage 3-continued-2) before falling through
 * to the original.
 *
 * Gated on the {@code blockEntityCache} toggle via
 * {@link OptimizeToggles#blockEntityCache()}. When the toggle is off, the
 * original runs unchanged.
 */
@Mixin(BlockEntityRenderState.class)
public abstract class BlockEntityRenderStateMixin {

    @WrapOperation(
            method = "extractBase",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/util/LightCoordsUtil;getLightCoords(Lnet/minecraft/world/level/BlockAndLightGetter;Lnet/minecraft/core/BlockPos;)I")
    )
    private static int lovisual$cachedLight(BlockAndLightGetter level, BlockPos pos, Operation<Integer> original,
                                           BlockEntity blockEntity, BlockEntityRenderState state,
                                           ModelFeatureRenderer.CrumblingOverlay crumbling) {
        if (!OptimizeToggles.blockEntityCache() || !(level instanceof Level world)) {
            return original.call(level, pos);
        }
        if (!(blockEntity instanceof CachedLight cache)) {
            return original.call(level, pos);
        }
        long tick = world.getGameTime();
        if (cache.lovisual$lightTick() != tick) {
            cache.lovisual$setLight(original.call(level, pos), tick);
        }
        return cache.lovisual$light();
    }
}
