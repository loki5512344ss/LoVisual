package dev.loki.lovisual.mixins.render.placement;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.placeanimation.PlaceAnimation;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** PlaceAnimation: грани соседей рядом с анимируемым блоком рисуются, пока тот скрыт из меша. */
@Mixin(ModelBlockRenderer.class)
public abstract class ModelBlockRendererPlaceMixin {

    @Inject(method = "shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true)
    private void lovisual$revealFacesNextToPlacingBlock(BlockAndTintGetter level, BlockState state, Direction direction,
                                                        BlockPos neighborPos, CallbackInfoReturnable<Boolean> cir) {
        PlaceAnimation module = Modules.get(PlaceAnimation.class);
        if (module != null && module.isHidden(neighborPos)) cir.setReturnValue(true);
    }
}
