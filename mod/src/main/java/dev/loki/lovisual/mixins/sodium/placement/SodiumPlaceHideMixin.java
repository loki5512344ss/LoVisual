package dev.loki.lovisual.mixins.sodium.placement;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.placeanimation.PlaceAnimation;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PlaceAnimation под Sodium: блок не попадает в меш. Приоритет ниже, чем у SodiumBlockRendererMixin,
 * чтобы отмена шла до push состояния поверхности и стек не терял баланс.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer", priority = 500)
public abstract class SodiumPlaceHideMixin {

    @Inject(method = "renderModel", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void lovisual$hidePlacingBlock(BlockStateModel model, BlockState state, BlockPos pos, BlockPos origin, CallbackInfo ci) {
        PlaceAnimation module = Modules.get(PlaceAnimation.class);
        if (module != null && module.isHidden(pos)) ci.cancel();
    }
}
