package dev.loki.lovisual.mixins.optimize.entity.parallel;

import java.util.List;
import java.util.Map;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Stage 3 step 8: exposes the part-tree internals the parallel model build
 * flattens once per model — the baked cubes and the ordered children map.
 */
@Mixin(ModelPart.class)
public interface ModelPartAccessor {

    @Accessor("cubes")
    List<ModelPart.Cube> lovisual$cubes();

    @Accessor("children")
    Map<String, ModelPart> lovisual$children();
}
