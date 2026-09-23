package dev.loki.lovisual.features.module.modules.visuals.scene.visual.chams;

import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;

public final class ChamsColorHelper {
    private ChamsColorHelper() {}

    public static float channel(int argb, int shift) {
        return ((argb >> shift) & 0xFF) / 255.0f;
    }

    public static float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }

    public static int forceOpaque(int argb) {
        return 0xFF000000 | (argb & 0x00FFFFFF);
    }

    public static int mixRgb(int a, int b, float t) {
        t = clamp01(t);
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;
        int rr = Math.round(ar + (br - ar) * t);
        int rg = Math.round(ag + (bg - ag) * t);
        int rb = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (rr << 16) | (rg << 8) | rb;
    }

    public static int rgb01(float r01, float g01) {
        int r = Math.round(clamp01(r01) * 255f);
        int g = Math.round(clamp01(g01) * 255f);
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    public static int materialFillRgb(int fillArgb, String visualColorSource) {
        if ("Theme".equals(visualColorSource)) {
            Themes.Theme theme = Theme.theme();
            return forceOpaque(mixRgb(theme.accent(), theme.textPrimary(), 0.18f));
        }
        return forceOpaque(fillArgb);
    }

    public static int metallicBaseRgb(String visualColorSource, int metallicBaseArgb) {
        if ("Theme".equals(visualColorSource)) {
            Themes.Theme theme = Theme.theme();
            return forceOpaque(mixRgb(theme.surfaceHover(), theme.accent(), 0.42f));
        }
        return forceOpaque(metallicBaseArgb);
    }

    public static int metallicHighlightRgb(String visualColorSource, int metallicHighlightArgb) {
        if ("Theme".equals(visualColorSource)) {
            Themes.Theme theme = Theme.theme();
            return forceOpaque(mixRgb(theme.textPrimary(), theme.accentSoft(), 0.28f));
        }
        return forceOpaque(metallicHighlightArgb);
    }

    public static int glowRgb(String effectColorSource, int glowArgb, int materialRgb) {
        if ("Custom".equals(effectColorSource)) return forceOpaque(glowArgb);
        if ("Theme".equals(effectColorSource)) return forceOpaque(Theme.theme().accentSoft());
        return forceOpaque(mixRgb(materialRgb, 0xFFFFFFFF, 0.25f));
    }

    public static int shadowRgb(String effectColorSource, int shadowArgb, int materialRgb) {
        if ("Custom".equals(effectColorSource)) return forceOpaque(shadowArgb);
        if ("Theme".equals(effectColorSource))
            return forceOpaque(mixRgb(Theme.theme().windowBg(), Theme.theme().accent(), 0.08f));
        return forceOpaque(mixRgb(materialRgb, 0xFF05070C, 0.72f));
    }
}
