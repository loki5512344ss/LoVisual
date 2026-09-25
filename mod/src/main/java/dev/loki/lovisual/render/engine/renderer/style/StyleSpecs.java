package dev.loki.lovisual.render.engine.renderer.style;

/**
 * Preset {@link StyleSpec} values per material. The glass numbers are lifted
 * verbatim from the legacy per-site helpers (HudRenderUtil large/small plates,
 * ClickGUI panel glass) so migrating call sites keeps visuals unchanged.
 */
public enum StyleSpecs {
    ;

    /** Base RGB of the legacy {@code GLASS_BG = 0x660C0C0C} fill. */
    public static final int GLASS_FILL_RGB = 0x0C0C0C;

    /** Liquid-glass material for a large or small plate (HudRenderUtil numbers). */
    public static StyleSpec glass(boolean large) {
        return new StyleSpec(
                SurfaceStyle.GLASS,
                large ? 13.0f : 11.0f,
                large ? -22.0f : -18.0f,
                1.0f,
                large ? 0.74f : 0.82f,
                large ? 0.34f : 0.38f,
                large ? 0.155f : 0.135f,
                0.0f,
                0xFFFFFFFF,
                0.6f,
                0.0f);
    }

    /** Preset for the given style; {@code AUTO} falls back to the large glass plate. */
    public static StyleSpec resolve(SurfaceStyle style) {
        SurfaceStyle s = style == null ? SurfaceStyle.AUTO : style;
        return switch (s) {
            case GLASS -> glass(true);
            case BLUR -> new StyleSpec(s, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0xFFFFFFFF, 0.0f, 1.0f);
            case FLAT -> new StyleSpec(s, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0xFFFFFFFF, 0.6f, 1.0f);
            case OUTLINE -> new StyleSpec(s, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0xFFFFFFFF, 1.0f, 1.0f);
            case NONE -> new StyleSpec(s, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 0, 0.0f, 0.0f);
            case AUTO -> glass(true);
        };
    }
}
