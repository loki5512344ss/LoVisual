package dev.loki.lovisual.mixins.optimize.block;

import dev.loki.lovisual.features.module.modules.misc.optimize.block.TickCached;
import net.minecraft.world.level.BaseSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Stage 3: implements {@link TickCached} on {@link BaseSpawner} — adds the
 * per-tick cache slot that {@link dev.loki.lovisual.features.module.modules.misc.optimize.block.SpawnerCache}
 * reads back to skip the per-frame {@code extractEntity} rebuild of the
 * spawner's display mob.
 *
 * <p>The vanilla {@code SpawnerRenderer.extractRenderState} rebuilds the
 * display entity's render state every frame for every spawner in view;
 * the display entity only changes on tick boundaries (spin speed, mob
 * rotation, etc.), so caching by tick removes that rebuild from the hot path.
 *
 */
@Mixin(BaseSpawner.class)
public abstract class BaseSpawnerMixin implements TickCached {

    @Unique
    private Object lovisual$cached;

    @Unique
    private long lovisual$cachedTick = Long.MIN_VALUE;

    @Override
    public Object lovisual$cached() {
        return lovisual$cached;
    }

    @Override
    public long lovisual$cachedTick() {
        return lovisual$cachedTick;
    }

    @Override
    public void lovisual$setCached(Object value, long tick) {
        lovisual$cached = value;
        lovisual$cachedTick = tick;
    }
}
