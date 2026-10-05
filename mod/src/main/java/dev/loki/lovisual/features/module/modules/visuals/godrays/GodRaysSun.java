package dev.loki.lovisual.features.module.modules.visuals.godrays;

import org.joml.Matrix4f;
import org.joml.Vector4f;

/** Sky state captured each frame plus the pure maths that turns it into a screen position and a fade. */
public final class GodRaysSun {
    /** Sun position on screen in 0..1 uv together with how high it stands (-1..1). */
    public record Screen(float u, float v, float elevation) {
    }

    private static final float HORIZON_FADE_START = 0.03f;
    private static final float HORIZON_FADE_END = 0.2f;
    private static final float OFF_SCREEN_RANGE = 0.55f;
    private static final float RAIN_FADE = 0.85f;
    private static final float SUNSET_GAIN = 0.8f;
    private static final float SUNSET_REFERENCE = 0.45f;
    private static final float WARMTH = 0.85f;
    private static final float W_EPSILON = 1.0E-6f;

    private static volatile float sunAngle;
    private static volatile int sunriseColor;
    private static volatile boolean valid;

    private GodRaysSun() {
    }

    public static void capture(float angle, int sunriseAndSunsetColor) {
        sunAngle = angle;
        sunriseColor = sunriseAndSunsetColor;
        valid = true;
    }

    public static void invalidate() {
        valid = false;
    }

    public static boolean isValid() {
        return valid;
    }

    public static float angle() {
        return sunAngle;
    }

    public static int sunrise() {
        return sunriseColor;
    }

    /** Projects the sun direction; null while it is below the horizon or behind the camera. */
    public static Screen project(float angle, Matrix4f viewProjection) {
        float elevation = (float) Math.cos(angle);
        if (elevation <= HORIZON_FADE_START) return null;
        Vector4f clip = new Vector4f(-(float) Math.sin(angle), elevation, 0.0f, 0.0f).mul(viewProjection);
        if (clip.w <= W_EPSILON) return null;
        return new Screen(clip.x / clip.w * 0.5f + 0.5f, clip.y / clip.w * 0.5f + 0.5f, elevation);
    }

    public static float fade(Screen sun, float rain, boolean sunsetBoost) {
        float horizon = smoothstep(HORIZON_FADE_START, HORIZON_FADE_END, sun.elevation());
        float overshoot = Math.max(Math.max(Math.abs(sun.u() - 0.5f) - 0.5f, 0.0f), Math.max(Math.abs(sun.v() - 0.5f) - 0.5f, 0.0f));
        float screen = 1.0f - smoothstep(0.0f, OFF_SCREEN_RANGE, overshoot);
        float weather = 1.0f - Math.max(0.0f, Math.min(rain, 1.0f)) * RAIN_FADE;
        float boost = sunsetBoost ? 1.0f + SUNSET_GAIN * (1.0f - Math.min(sun.elevation() / SUNSET_REFERENCE, 1.0f)) : 1.0f;
        return horizon * screen * weather * boost;
    }

    /** One tint channel: the ray colour pulled towards the sunrise/sunset glow while there is one. */
    public static float tintChannel(int rayChannel, int glowChannel, int glowAlpha) {
        float warmth = glowAlpha / 255.0f * WARMTH;
        float glow = glowChannel / 255.0f;
        return rayChannel / 255.0f * (1.0f - warmth + glow * warmth);
    }

    static float smoothstep(float from, float to, float x) {
        float t = Math.max(0.0f, Math.min((x - from) / (to - from), 1.0f));
        return t * t * (3.0f - 2.0f * t);
    }
}
