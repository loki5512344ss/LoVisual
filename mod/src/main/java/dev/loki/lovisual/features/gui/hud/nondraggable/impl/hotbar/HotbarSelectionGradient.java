package dev.loki.lovisual.features.gui.hud.nondraggable.impl.hotbar;

import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.math.ColorMath;

/**
 * Resolves the two-stop gradient used for the hotbar selection stroke and fill.
 * Prefers the user's manual select colours when they opted in, falls back to the
 * active theme's card gradient, and finally to flat RGB values. All helpers are
 * pure so {@link HotbarRenderer} can call them per-frame without state.
 */
final class HotbarSelectionGradient {

    private static final float DEFAULT_ANGLE_DEG = 90.0f;

    private HotbarSelectionGradient() {
    }

    /** Two-stop gradient used for the selection stroke + fill this frame. */
    record SelectionGradient(int startArgb, int endArgb, float angleDeg) {
    }

    /**
     * Resolve the two-stop gradient used for the selection stroke. Uses the manual
     * select colours when the user opted in; otherwise inherits the active theme's
     * card gradient. Alpha is applied uniformly to both stops.
     */
    static SelectionGradient resolve(CustomHotbar bm, int alpha) {
        int a = Math.max(0, Math.min(255, alpha));
        if (bm.customSelectColorEnabled()) {
            return new SelectionGradient(
                    ColorMath.premultiplyAlpha(bm.getHotbarSelectRgb(), a),
                    ColorMath.premultiplyAlpha(bm.getHotbarSelectEndRgb(), a),
                    DEFAULT_ANGLE_DEG);
        }
        Themes.ThemeEntry entry = Theme.currentEntry();
        Themes.GradientSpec gradient = entry != null ? entry.cardGradient() : null;
        if (gradient != null && gradient.enabled()) {
            return new SelectionGradient(
                    withAlpha(gradient.start(), a),
                    withAlpha(gradient.end(), a),
                    gradient.angleDeg());
        }
        return new SelectionGradient(
                ColorMath.premultiplyAlpha(bm.getHotbarSelectRgb(), a),
                ColorMath.premultiplyAlpha(bm.getHotbarSelectEndRgb(), a),
                DEFAULT_ANGLE_DEG);
    }

    /** Preserve the source ARGB's own alpha ratio while scaling it into {@code alpha}. */
    private static int withAlpha(int argb, int alpha) {
        int sourceAlpha = (argb >>> 24) & 0xFF;
        int outAlpha = Math.round(sourceAlpha * (alpha / 255.0f));
        return ColorMath.premultiplyAlpha(argb & 0x00FFFFFF, outAlpha);
    }
}
