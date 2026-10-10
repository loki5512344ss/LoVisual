package dev.loki.lovisual.mixins.optimize.entity;

import dev.loki.lovisual.features.module.modules.misc.optimize.entity.CachedName;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Stage 3: implements {@link CachedName} on {@link Entity} — adds the cached
 * name-tag fields (name + tick) that {@link EntityRendererNameMixin} populates
 * and reads back. Vanilla {@code Entity.getDisplayName()} rebuilds the team
 * prefix + name + suffix on every call; renderers call it once per frame per
 * entity in view, so caching by tick cuts the per-frame cost.
 *
 * <p>Ported from {@code zako.opt.mixin.entity.EntityNameMixin} (ZakoOpt,
 * LGPL-3.0). Original author: Zako — https://t.me/StarikZako. Field/method
 * prefix changed from {@code zakoopt$} to {@code lovisual$}.
 */
@Mixin(Entity.class)
public abstract class EntityNameMixin implements CachedName {

    @Unique
    private Component lovisual$name;

    @Unique
    private long lovisual$nameTick = Long.MIN_VALUE;

    @Override
    public Component lovisual$name() {
        return lovisual$name;
    }

    @Override
    public long lovisual$nameTick() {
        return lovisual$nameTick;
    }

    @Override
    public void lovisual$setName(Component name, long tick) {
        lovisual$name = name;
        lovisual$nameTick = tick;
    }
}
