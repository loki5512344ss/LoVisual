package dev.loki.lovisual.mixins.render.item.bounds;

import java.util.function.Supplier;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Stage 3 step 5: exposes the layer internals {@link ItemStackRenderStateMixin}
 * keys the shared {@code ItemBounds} cache on — the extents supplier identity,
 * the item transform and the local matrix whose identity bit short-circuits
 * the cache.
 */
@Mixin(ItemStackRenderState.LayerRenderState.class)
public interface ItemLayerAccessor {

    @Accessor("extents")
    Supplier<Vector3fc[]> lovisual$extents();

    @Accessor("itemTransform")
    ItemTransform lovisual$itemTransform();

    @Accessor("localTransform")
    Matrix4f lovisual$localTransform();
}
