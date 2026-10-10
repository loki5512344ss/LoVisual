package dev.loki.lovisual.features.module.modules.misc.optimize.block;

import net.minecraft.world.level.block.state.BlockState;

/**
 * Mixin-contract: cached validity and "should-render" distance check on a
 * {@code BlockEntity}. {@code BlockEntityType.isValid(state)} is called every
 * frame per block-entity in view but only changes on a state transition —
 * cached by identity on {@link #lovisual$validState()} / {@link #lovisual$valid()}.
 * The companion {@code inDistance} methods cache
 * {@code BlockEntityRenderer.shouldRender}'s distance check per tick.
 */
public interface CachedValidity {
    BlockState lovisual$validState();

    boolean lovisual$valid();

    void lovisual$setValid(BlockState state, boolean valid);

    long lovisual$distanceTick();

    boolean lovisual$inDistance();

    void lovisual$setInDistance(boolean inDistance, long tick);
}
