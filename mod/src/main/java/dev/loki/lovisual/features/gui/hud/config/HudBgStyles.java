package dev.loki.lovisual.features.gui.hud.config;

import dev.loki.lovisual.config.style.StyleConfig;
import dev.loki.lovisual.render.engine.renderer.style.SurfaceStyle;

/**
 * Shared option ids and resolution for HUD widget background effects.
 * Replaces the per-widget {@code EFFECT_NONE/BLUR/GLASS} string copies;
 * {@link #AUTO} follows the global {@link StyleConfig} selection.
 */
public final class HudBgStyles {

    public static final String NONE = "None";
    public static final String BLUR = "Blur";
    public static final String GLASS = "Glass";
    public static final String AUTO = "Auto";

    private HudBgStyles() {
    }

    /** Option list for every bg-effect ModeValue: legacy trio plus Auto. */
    public static String[] options() {
        return new String[]{NONE, BLUR, GLASS, AUTO};
    }

    /** Resolves a raw option id; {@code Auto} maps to the global style. */
    public static SurfaceStyle resolve(String effect) {
        if (GLASS.equals(effect)) return SurfaceStyle.GLASS;
        if (BLUR.equals(effect)) return SurfaceStyle.BLUR;
        if (AUTO.equals(effect)) return StyleConfig.get().style();
        return SurfaceStyle.NONE;
    }

    public static boolean isNone(String effect) {
        return resolve(effect) == SurfaceStyle.NONE;
    }

    public static boolean isBlur(String effect) {
        return resolve(effect) == SurfaceStyle.BLUR;
    }

    public static boolean isGlass(String effect) {
        return resolve(effect) == SurfaceStyle.GLASS;
    }

    public static boolean hasEffect(String effect) {
        return !isNone(effect);
    }

    /**
     * Normalizes an effect id for the JS compact-stat layer, which only knows
     * Blur/Glass/None; FLAT/OUTLINE fall back to a plain fill there.
     */
    public static String scriptEffect(String effect) {
        return switch (resolve(effect)) {
            case GLASS -> GLASS;
            case BLUR -> BLUR;
            case FLAT, OUTLINE, NONE, AUTO -> NONE;
        };
    }
}
