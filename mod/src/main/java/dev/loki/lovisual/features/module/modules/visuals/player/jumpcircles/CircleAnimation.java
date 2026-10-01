package dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles;

/** Pure timing and easing maths for a single jump circle; no Minecraft dependencies. */
public final class CircleAnimation {
    public static final float BASE_LIFETIME_MS = 2400f;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float FADE_IN = 0.08f;
    private static final float SCALE_PULSE = 0.04f;
    private static final float ALPHA_PULSE = 0.15f;
    private static final float SWAY = 0.15f;
    private static final float SPIN_PER_SECOND = 0.8f;
    private static final float WAVE_RADIANS_PER_SECOND = 3f;

    private CircleAnimation() {
    }

    public static float lifetimeMs(float speed) {
        return BASE_LIFETIME_MS / Math.max(0.1f, speed);
    }

    public static float progress(float ageMs, float lifetimeMs) {
        return clamp01(ageMs / lifetimeMs);
    }

    public static float easeOutCubic(float p) {
        float inv = 1f - clamp01(p);
        return 1f - inv * inv * inv;
    }

    /** Eased growth to {@code maxRadius} with a slight breathing; never exceeds {@code maxRadius}. */
    public static float radius(float progress, float maxRadius) {
        float breathe = 0.5f + 0.5f * (float) Math.sin(progress * TWO_PI * 4f);
        return maxRadius * easeOutCubic(progress) * (1f - SCALE_PULSE + SCALE_PULSE * breathe);
    }

    /** Short fade-in, then a fade-out whose steepness grows with {@code fadeSpeed}; result is in [0,1]. */
    public static float alpha(float progress, float fadeSpeed) {
        float in = clamp01(progress / FADE_IN);
        float out = clamp01((1f - progress) * Math.max(0f, fadeSpeed));
        float flicker = 1f - ALPHA_PULSE + ALPHA_PULSE * (0.5f + 0.5f * (float) Math.sin(progress * TWO_PI * 5f));
        return clamp01(in * out * flicker);
    }

    /** Steady spin over time plus a small sway driven by progress, in radians. */
    public static float angle(float ageSeconds, float rotateSpeed, float progress) {
        return ageSeconds * rotateSpeed * SPIN_PER_SECOND + SWAY * (float) Math.sin(progress * TWO_PI * 2f);
    }

    public static float wavePhase(float ageSeconds) {
        return ageSeconds * WAVE_RADIANS_PER_SECOND;
    }

    private static float clamp01(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
