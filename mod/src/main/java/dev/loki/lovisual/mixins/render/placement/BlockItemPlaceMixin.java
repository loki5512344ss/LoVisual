package dev.loki.lovisual.mixins.render.placement;

import com.llamalad7.mixinextras.sugar.Local;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.placeanimation.PlaceAnimation;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** PlaceAnimation: старт анимации сразу после того, как блок поставлен на клиентском уровне. */
@Mixin(BlockItem.class)
public abstract class BlockItemPlaceMixin {

    @Inject(method = "place(Lnet/minecraft/world/item/context/BlockPlaceContext;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/BlockItem;updateBlockStateFromTag(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private void lovisual$startPlaceAnimation(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir,
                                              @Local Level level, @Local BlockPos pos, @Local Player player,
                                              @Local(ordinal = 0) BlockState placementState,
                                              @Local(ordinal = 1) BlockState placedState) {
        if (!(level instanceof ClientLevel clientLevel) || !placedState.is(placementState.getBlock())) return;
        PlaceAnimation module = Modules.get(PlaceAnimation.class);
        if (module != null) module.onBlockPlaced(clientLevel, pos, placedState, player, context.getHand());
    }
}
