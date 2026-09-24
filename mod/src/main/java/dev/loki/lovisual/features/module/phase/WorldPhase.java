package dev.loki.lovisual.features.module.phase;

public enum WorldPhase {
    NONE,

    BEFORE_ENTITIES,     // WorldRenderEvents.BEFORE_ENTITIES
    AFTER_ENTITIES,      // WorldRenderEvents.AFTER_ENTITIES
    BEFORE_TRANSLUCENT,  // WorldRenderEvents.BEFORE_TRANSLUCENT
    END_MAIN,            // WorldRenderEvents.END_MAIN (аналог старого LAST)
    AFTER_POST_PROCESS   // After PRE_HAND postprocess, before hand render
}
