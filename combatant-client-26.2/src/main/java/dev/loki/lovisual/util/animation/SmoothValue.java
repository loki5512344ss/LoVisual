/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.util.animation;

/**
 * Frame-rate independent smooth 0..1 transition value driven by a boolean state.
 * <p>
 * The contract is intentionally strict so animations can never fight each other:
 * the owner computes the target once per frame with {@link #setTarget(boolean)}
 * and advances the value exactly once per frame with {@link #update(float)}.
 * Renderers only read {@link #value()}. The value approaches the target with
 * exponential easing {@code value += (target - value) * (1 - exp(-speed * delta))},
 * which is monotonic, symmetric for appear/disappear, and identical in wall-clock
 * time regardless of frame rate.
 * <p>
 * This class is pure Java: no Minecraft types, safe to unit test.
 */
public final class SmoothValue {

    /**
     * When the remaining distance to the target drops below this, the value snaps
     * exactly onto the target so it converges in finite time instead of forever
     * trailing a microscopic difference.
     */
    private static final float CONVERGENCE_EPSILON = 1.0e-4f;

    private final float speed;
    private float value;
    private float target;

    /**
     * @param speed approach rate in 1/delta units; higher is faster.
     *              With delta measured in ticks (20 per second), a speed of
     *              {@code s} needs roughly {@code ln(100) / s} ticks to close 99% of a transition.
     */
    public SmoothValue(float speed) {
        this(speed, 0.0f);
    }

    public SmoothValue(float speed, float initialValue) {
        if (!isFinite(speed) || speed <= 0.0f) {
            throw new IllegalArgumentException("speed must be finite and positive: " + speed);
        }
        this.speed = speed;
        this.value = clamp01(initialValue);
        this.target = this.value;
    }

    /**
     * Sets the goal for the next {@link #update(float)} calls: 1 when the animated
     * state is active, 0 when it is not. Does not move the value by itself.
     */
    public void setTarget(boolean active) {
        target = active ? 1.0f : 0.0f;
    }

    /**
     * Advances the value one step toward the target. Call exactly once per frame.
     *
     * @param delta elapsed time since the previous update, in the same units the
     *              speed was configured for (ticks in this client). Non-positive
     *              or broken deltas are ignored.
     */
    public void update(float delta) {
        if (!isFinite(delta) || delta <= 0.0f) {
            return;
        }

        float factor = 1.0f - (float) Math.exp(-speed * delta);
        if (factor <= 0.0f) {
            return;
        }

        value += (target - value) * factor;
        value = clamp01(value);

        if (Math.abs(target - value) <= CONVERGENCE_EPSILON) {
            value = target;
        }
    }

    /**
     * Teleports both value and target: no transition, the next frame renders settled.
     */
    public void setInstant(boolean active) {
        target = active ? 1.0f : 0.0f;
        value = target;
    }

    /** Current smoothed position in 0..1. */
    public float value() {
        return value;
    }

    /** Current goal in 0..1 as last set by {@link #setTarget(boolean)}. */
    public float target() {
        return target;
    }

    /** True once the value has fully reached the target. */
    public boolean isSettled() {
        return value == target;
    }

    private static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }

    private static boolean isFinite(float v) {
        return !Float.isNaN(v) && !Float.isInfinite(v);
    }
}
