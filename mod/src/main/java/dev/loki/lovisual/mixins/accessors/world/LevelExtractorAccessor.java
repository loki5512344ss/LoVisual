package dev.loki.lovisual.mixins.accessors.world;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelExtractor.class)
public interface LevelExtractorAccessor {

    @Invoker("extractEntity")
    EntityRenderState lovisual$invokeExtractEntity(Entity entity, float tickDelta);
}
