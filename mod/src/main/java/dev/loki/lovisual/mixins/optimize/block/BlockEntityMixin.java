package dev.loki.lovisual.mixins.optimize.block;

import dev.loki.lovisual.features.module.modules.misc.optimize.block.CachedLight;
import dev.loki.lovisual.features.module.modules.misc.optimize.block.CachedValidity;
import dev.loki.lovisual.features.module.modules.misc.optimize.block.TickCached;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Stage 3: implements {@link TickCached}, {@link CachedLight} and
 * {@link CachedValidity} on {@link BlockEntity} — adds the per-tick cache
 * slot (object + tick), the per-tick block-light lookups, the per-tick
 * should-render distance check, and the per-state validity cache.
 *
 * <p>The vanilla {@code BlockEntityType.isValid(state)} is called every
 * frame per block-entity in view but only changes on a state transition;
 * {@code LightCoordsUtil.getLightCoords(level, pos)} walks the chunk's light
 * section data per call; {@code BlockEntityRenderer.shouldRender} does a
 * distance check that only changes when the entity moves across the cull
 * boundary. All three are cached via the interface fields implemented here.
 *
 * <p>Ported from {@code zako.opt.mixin.block.BlockEntityMixin} (ZakoOpt,
 * LGPL-3.0). Original author: Zako — https://t.me/StarikZako. Field/method
 * prefix changed from {@code zakoopt$} to {@code lovisual$}.
 */
@Mixin(BlockEntity.class)
public abstract class BlockEntityMixin implements TickCached, CachedLight, CachedValidity {

    // --- TickCached ---
    @Unique
    private Object lovisual$cached;
    @Unique
    private long lovisual$cachedTick = Long.MIN_VALUE;

    // --- CachedLight ---
    @Unique
    private int lovisual$light;
    @Unique
    private long lovisual$lightTick = Long.MIN_VALUE;
    @Unique
    private int lovisual$pairLight;
    @Unique
    private long lovisual$pairLightTick = Long.MIN_VALUE;

    // --- CachedValidity ---
    @Unique
    private BlockState lovisual$validState;
    @Unique
    private boolean lovisual$valid;
    @Unique
    private long lovisual$distanceTick = Long.MIN_VALUE;
    @Unique
    private boolean lovisual$inDistance;

    // --- TickCached ---

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

    // --- CachedLight ---

    @Override
    public int lovisual$light() {
        return lovisual$light;
    }

    @Override
    public long lovisual$lightTick() {
        return lovisual$lightTick;
    }

    @Override
    public void lovisual$setLight(int light, long tick) {
        lovisual$light = light;
        lovisual$lightTick = tick;
    }

    @Override
    public int lovisual$pairLight() {
        return lovisual$pairLight;
    }

    @Override
    public long lovisual$pairLightTick() {
        return lovisual$pairLightTick;
    }

    @Override
    public void lovisual$setPairLight(int light, long tick) {
        lovisual$pairLight = light;
        lovisual$pairLightTick = tick;
    }

    // --- CachedValidity ---

    @Override
    public BlockState lovisual$validState() {
        return lovisual$validState;
    }

    @Override
    public boolean lovisual$valid() {
        return lovisual$valid;
    }

    @Override
    public void lovisual$setValid(BlockState state, boolean valid) {
        lovisual$validState = state;
        lovisual$valid = valid;
    }

    @Override
    public long lovisual$distanceTick() {
        return lovisual$distanceTick;
    }

    @Override
    public boolean lovisual$inDistance() {
        return lovisual$inDistance;
    }

    @Override
    public void lovisual$setInDistance(boolean inDistance, long tick) {
        lovisual$inDistance = inDistance;
        lovisual$distanceTick = tick;
    }
}
