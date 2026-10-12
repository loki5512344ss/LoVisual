package dev.loki.lovisual.mixins.render.item.bounds;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.loki.lovisual.render.engine.optimize.entity.ItemBounds;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Stage 3 step 5: single-layer item model bounds served from
 * {@link ItemBounds} instead of recomputed every frame. The vanilla path
 * walks the layer's extents supplier; the cache key is that supplier's
 * identity, which is stable per baked model. Multi-layer items and items
 * with a non-identity local transform keep the vanilla computation.
 */
@Mixin(ItemStackRenderState.class)
public abstract class ItemStackRenderStateMixin {

    @Shadow
    private int activeLayerCount;

    @Shadow
    private ItemStackRenderState.LayerRenderState[] layers;

    @WrapMethod(method = "getModelBoundingBox")
    private AABB lovisual$sharedBounds(Operation<AABB> original) {
        if (!ItemBounds.enabled() || activeLayerCount != 1) {
            return original.call();
        }
        ItemLayerAccessor layer = (ItemLayerAccessor) layers[0];
        if ((layer.lovisual$localTransform().properties() & Matrix4fc.PROPERTY_IDENTITY) == 0) {
            return original.call();
        }
        return ItemBounds.get(layer.lovisual$extents(), layer.lovisual$itemTransform(), original::call);
    }
}
