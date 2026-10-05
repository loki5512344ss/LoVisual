package dev.loki.lovisual.features.theme;

import dev.loki.lovisual.features.theme.Themes.GradientSpec;
import dev.loki.lovisual.features.theme.Themes.Theme;
import dev.loki.lovisual.features.theme.Themes.ThemeEntry;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;

/**
 * Stateless color / gradient math shared by {@link ThemesTransition} (frame blending)
 * and {@link ThemesProfileCodec} (defaults for missing keys). Kept in a sibling
 * folder so the main theme directory stays within the four-file-per-directory limit;
 * the package declaration is unchanged so package-private callers keep working.
 */
final class ThemesBlending {

    private ThemesBlending() { }

    /** Non-animated gradient used as the "no gradient" fallback everywhere in the theme system. */
    static GradientSpec flatGradient(int color) {
        return new GradientSpec(false, color, color, 90f);
    }

    /** Soft top-lit header gradient derived from a single base color. */
    static GradientSpec subtleHeaderGradient(int color) {
        int start = adjustBrightness(color, 0.08f);
        int end = adjustBrightness(color, -0.06f);
        return new GradientSpec(true, start, end, 90f);
    }

    static int adjustBrightness(int argb, float delta) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        r = clampChannel(r + Math.round((delta >= 0 ? (255 - r) : r) * delta));
        g = clampChannel(g + Math.round((delta >= 0 ? (255 - g) : g) * delta));
        b = clampChannel(b + Math.round((delta >= 0 ? (255 - b) : b) * delta));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    static int clampChannel(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    /** Per-channel linear interpolation between two packed ARGB colors. */
    static int mixArgb(int from, int to, float t) {
        t = AnimationUtility.clamp01(t);
        int fa = (from >>> 24) & 0xFF, fr = (from >>> 16) & 0xFF, fg = (from >>> 8) & 0xFF, fb = from & 0xFF;
        int ta = (to >>> 24) & 0xFF, tr = (to >>> 16) & 0xFF, tg = (to >>> 8) & 0xFF, tb = to & 0xFF;
        return (clampChannel(Math.round(fa + (ta - fa) * t)) << 24)
                | (clampChannel(Math.round(fr + (tr - fr) * t)) << 16)
                | (clampChannel(Math.round(fg + (tg - fg) * t)) << 8)
                | clampChannel(Math.round(fb + (tb - fb) * t));
    }

    /** Shortest-arc angle interpolation so 350°→10° does not spin the long way around. */
    static float mixAngle(float from, float to, float t) {
        float delta = ((to - from + 540.0f) % 360.0f) - 180.0f;
        float out = from + delta * AnimationUtility.clamp01(t);
        out %= 360.0f;
        return out < 0.0f ? out + 360.0f : out;
    }

    /** Linear blend of the 12 semantic color slots of a {@link Theme}. */
    static Theme blendTheme(Theme from, Theme to, float t) {
        if (from == null) from = ThemePresets1.CLASSIC;
        if (to == null) to = ThemePresets1.CLASSIC;
        return new Theme(
                mixArgb(from.windowBg(), to.windowBg(), t),
                mixArgb(from.windowHeader(), to.windowHeader(), t),
                mixArgb(from.windowStroke(), to.windowStroke(), t),
                mixArgb(from.surface(), to.surface(), t),
                mixArgb(from.surfaceHover(), to.surfaceHover(), t),
                mixArgb(from.cardEnabled(), to.cardEnabled(), t),
                mixArgb(from.cardDisabled(), to.cardDisabled(), t),
                mixArgb(from.textPrimary(), to.textPrimary(), t),
                mixArgb(from.textMuted(), to.textMuted(), t),
                mixArgb(from.accent(), to.accent(), t),
                mixArgb(from.accentSoft(), to.accentSoft(), t),
                mixArgb(from.strokeSoft(), to.strokeSoft(), t)
        );
    }

    /** Blend a pair of gradient specs; the result stays enabled if either endpoint is enabled. */
    static GradientSpec blendGradient(GradientSpec from, GradientSpec to, float t) {
        if (from == null) from = flatGradient(ThemePresets1.CLASSIC.accent());
        if (to == null) to = flatGradient(ThemePresets1.CLASSIC.accent());
        boolean enabled = from.enabled() || to.enabled();
        return new GradientSpec(enabled, mixArgb(from.start(), to.start(), t), mixArgb(from.end(), to.end(), t), mixAngle(from.angleDeg(), to.angleDeg(), t));
    }

    /** Blend two full {@link ThemeEntry} objects including every gradient slot; used during transitions. */
    static ThemeEntry blendEntry(ThemeEntry from, ThemeEntry to, float t) {
        return new ThemeEntry(
                to.getId(), to.name(), to.builtin(),
                blendTheme(from.theme(), to.theme(), t),
                blendGradient(from.windowGradient(), to.windowGradient(), t),
                blendGradient(from.headerGradient(), to.headerGradient(), t),
                blendGradient(from.surfaceGradient(), to.surfaceGradient(), t),
                blendGradient(from.cardGradient(), to.cardGradient(), t),
                blendGradient(from.strokeGradient(), to.strokeGradient(), t),
                from.backdrop().lerp(to.backdrop(), t)
        );
    }
}
