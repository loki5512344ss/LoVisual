package dev.loki.lovisual.features.module.modules.misc.optimize.entity;

import net.minecraft.world.entity.Entity;

/**
 * Mixin-contract: back-reference the source {@link Entity} on its
 * {@code EntityRenderState}. Render-states are rebuilt every frame, so they
 * can't keep their own pointer to the entity — the
 * {@code EntityRenderer.createRenderState} mixin captures it once per state
 * construction via {@link #lovisual$entity(Entity)} and downstream renderers
 * (model feature renderer, item feature renderer, future LOD mixins) read it
 * back via {@link #lovisual$entity()} to reach the live entity for animation
 * freeze, LOD distance checks, etc.
 */
public interface StateEntity {
    Entity lovisual$entity();

    void lovisual$entity(Entity entity);
}
