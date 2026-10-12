package dev.loki.lovisual.features.module.modules.misc.optimize.world;

import dev.loki.lovisual.render.engine.optimize.OptimizeToggles;

/**
 * Per-frame state for the entity-outline skip optimization. The vanilla
 * pipeline clears the entity-outline target and runs {@code doEntityOutline}
 * every frame even when no glowing entity is on screen. This tracker flips
 * to {@code false} when the most recent frame had at least one glowing
 * entity, and the {@code LevelRendererOutlineMixin} reads it to skip the
 * clear + outline render on frames with no glowing entity.
 *
 * <p>Render-thread only. The state is reset at the start of every frame
 * ({@link #beginFrame()}), and {@link #markGlowing()} is called from the
 * outline-target-clear {@code WrapOperation} when the renderer actually
 * touches the outline target.
 *
 * The state is lifted out of the mixin so the mixin stays a thin wrapper.
 */
public enum OutlineTracker {
    ;

    private static volatile boolean glowingThisFrame;
    private static volatile boolean dirty = true;

    /** True when the outline skip is engaged this frame. */
    public static boolean enabled() {
        return OptimizeToggles.microOpts();
    }

    /** Called at the start of every frame — clears the per-frame glowing flag. */
    public static void beginFrame() {
        glowingThisFrame = false;
    }

    /** Called when the renderer touches the outline target this frame. */
    public static void markGlowing() {
        glowingThisFrame = true;
        dirty = true;
    }

    /** True when at least one glowing entity was rendered this frame. */
    public static boolean glowingThisFrame() {
        return glowingThisFrame;
    }

    /** True when the outline target needs a clear this frame (first dirty frame after a glowing one). */
    public static boolean needsClear() {
        if (dirty) return true;
        return glowingThisFrame;
    }

    /** Called after the clear completes — the target is no longer dirty until the next glowing frame. */
    public static void markCleared() {
        dirty = false;
    }

    /** True when {@code doEntityOutline} should be skipped this frame. */
    public static boolean shouldSkipOutlineRender() {
        if (!enabled()) return false;
        return !glowingThisFrame;
    }

    /** Test-only: reset all state. */
    static void reset() {
        glowingThisFrame = false;
        dirty = true;
    }

    /** Test-only: peek the glowing flag. */
    static boolean glowingThisFrameRaw() {
        return glowingThisFrame;
    }

    /** Test-only: peek the dirty flag. */
    static boolean dirtyRaw() {
        return dirty;
    }
}
