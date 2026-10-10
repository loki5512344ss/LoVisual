package dev.loki.lovisual.features.module.modules.misc.optimize.block;

/**
 * Mixin-contract: per-tick cached block-light lookups on a {@code BlockEntity}.
 * The vanilla {@code LightCoordsUtil.getLightCoords(level, pos)} call walks
 * the chunk's light section data every frame per block-entity in view;
 * caching it once per tick (block-light only changes on tick boundaries)
 * removes that walk from the hot path. The companion {@code pairLight}
 * methods cache the second lookup done for double-chests.
 */
public interface CachedLight {
    int lovisual$light();

    long lovisual$lightTick();

    void lovisual$setLight(int light, long tick);

    int lovisual$pairLight();

    long lovisual$pairLightTick();

    void lovisual$setPairLight(int light, long tick);
}
