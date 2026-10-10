package dev.loki.lovisual.features.module.modules.misc.optimize.entity;

import net.minecraft.network.chat.Component;

/**
 * Mixin-contract: cached rebuilt name tag for an entity.
 *
 * <p>The vanilla {@code Entity.getDisplayName()} rebuilds a {@link Component}
 * (team prefix + name + suffix) every time it's called. Renderers call it once
 * per frame per entity in view, which is wasteful — the team/name/suffix tuple
 * only changes on tick boundaries. Mixins on {@code Entity} implement this
 * interface, expose the cache slot via {@link #lovisual$name()}, {@link #lovisual$nameTick()}
 * and {@link #lovisual$setName(Component, long)}, and the
 * {@code EntityRenderer.getNameTag} wrapper fills it once per tick and reuses
 * the cached value on intermediate frames.
 */
public interface CachedName {
    Component lovisual$name();

    long lovisual$nameTick();

    void lovisual$setName(Component name, long tick);
}
