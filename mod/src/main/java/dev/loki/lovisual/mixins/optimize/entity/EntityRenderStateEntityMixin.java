package dev.loki.lovisual.mixins.optimize.entity;

import dev.loki.lovisual.features.module.modules.misc.optimize.entity.StateEntity;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Stage 3: implements {@link StateEntity} on {@link EntityRenderState} — adds
 * the back-reference field to the source {@link Entity}. Render-states are
 * rebuilt every frame, so they can't keep their own pointer — the
 * {@link EntityRendererStateMixin} captures it once per state construction
 * via {@code createRenderState} and downstream renderers read it back.
 *
 */
@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateEntityMixin implements StateEntity {

    @Unique
    private Entity lovisual$entity;

    @Override
    public Entity lovisual$entity() {
        return lovisual$entity;
    }

    @Override
    public void lovisual$entity(Entity entity) {
        lovisual$entity = entity;
    }
}
