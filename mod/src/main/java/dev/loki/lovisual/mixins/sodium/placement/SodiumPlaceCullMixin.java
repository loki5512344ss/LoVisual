package dev.loki.lovisual.mixins.sodium.placement;

import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.placeanimation.PlaceAnimation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** PlaceAnimation под Sodium: соседние грани рисуются, пока анимируемый блок скрыт из меша. */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.render.model.AbstractBlockRenderContext")
public abstract class SodiumPlaceCullMixin {

    @Shadow(remap = false)
    @Final
    private BlockPos.MutableBlockPos cachedPositionObject;

    @Inject(method = "shouldDrawSide(Lnet/minecraft/core/Direction;)Z",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/BlockAndTintGetter;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
                    shift = At.Shift.BEFORE),
            cancellable = true, remap = false, require = 0)
    private void lovisual$revealFacesNextToPlacingBlock(Direction direction, CallbackInfoReturnable<Boolean> cir) {
        PlaceAnimation module = Modules.get(PlaceAnimation.class);
        if (module != null && module.isHidden(cachedPositionObject)) cir.setReturnValue(true);
    }
}
