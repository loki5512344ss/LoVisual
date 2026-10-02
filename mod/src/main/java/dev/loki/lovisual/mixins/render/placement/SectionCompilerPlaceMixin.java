package dev.loki.lovisual.mixins.render.placement;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.visuals.placeanimation.PlaceAnimation;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** PlaceAnimation: блок, который сейчас анимируется, не попадает в меш секции и не закрывает соседей. */
@Mixin(SectionCompiler.class)
public abstract class SectionCompilerPlaceMixin {
    private static final String COMPILE = "compile(Lnet/minecraft/core/SectionPos;Lnet/minecraft/client/renderer/chunk/RenderSectionRegion;Lcom/mojang/blaze3d/vertex/VertexSorting;Lnet/minecraft/client/renderer/SectionBufferBuilderPack;)Lnet/minecraft/client/renderer/chunk/SectionCompiler$Results;";

    @ModifyExpressionValue(method = COMPILE,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"))
    private RenderShape lovisual$hidePlacingBlock(RenderShape original, @Local(ordinal = 2) BlockPos pos) {
        return original == RenderShape.MODEL && lovisual$hidden(pos) ? RenderShape.INVISIBLE : original;
    }

    @ModifyExpressionValue(method = COMPILE,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender()Z"))
    private boolean lovisual$unblockPlacingBlock(boolean original, @Local(ordinal = 2) BlockPos pos) {
        return original && !lovisual$hidden(pos);
    }

    private static boolean lovisual$hidden(BlockPos pos) {
        PlaceAnimation module = Modules.get(PlaceAnimation.class);
        return module != null && module.isHidden(pos);
    }
}
