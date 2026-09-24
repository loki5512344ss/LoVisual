package dev.loki.lovisual.render.engine.core.frame;

/**
 * Backend-visible lifecycle state for one Minecraft-presented frame.
 */
public enum FrameLifecycle {
    IDLE,
    RECORDING,
    SUBMITTED,
    PRESENTED
}
