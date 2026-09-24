package dev.loki.lovisual.features.gui.mainmenu;

import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.color.RenderColor;

/**
 * Shared text primitives for the LoVisual main menu. Wraps the {@link TextRenderer}
 * begin/end measure-and-draw cycle so painter code stays focused on layout + colour.
 * Lives in the same package as {@link LoVisualMainMenuScreen}; kept as a separate
 * class so the screen file stays within the two-hundred-line cap.
 */
final class MainMenuTextUtil {

    private MainMenuTextUtil() { }

    static float measureWidth(TextRenderer renderer, String text, float size) {
        if (renderer == null || text == null) return 0f;
        renderer.begin(size, true, false);
        float width = (float) renderer.getWidth(text, false);
        renderer.end();
        return width;
    }

    static float measureHeight(TextRenderer renderer, float size) {
        if (renderer == null) return 0f;
        renderer.begin(size, true, false);
        float height = (float) renderer.getHeight(false);
        renderer.end();
        return height;
    }

    static void drawText(TextRenderer renderer, String text, float x, float y, float size, int argb) {
        if (renderer == null || text == null || text.isEmpty()) return;
        renderer.setAlpha(1.0);
        renderer.begin(size, false, false);
        renderer.render(text, x, y, new RenderColor(argb), false);
        renderer.end();
    }

    static void drawCenteredText(TextRenderer renderer, String text, float centerX, float y, float size, int argb) {
        float x = centerX - measureWidth(renderer, text, size) * 0.5f;
        drawText(renderer, text, x, y, size, argb);
    }

    static void drawLiquidGlassText(TextRenderer renderer, String text, float x, float y, float size, int argb) {
        if (renderer == null || text == null || text.isEmpty()) return;
        renderer.setAlpha(1.0);
        renderer.begin(size, false, true);
        renderer.renderLiquidGlass(text, x, y, new RenderColor(argb), false);
        renderer.end();
    }

    static void drawCenteredLiquidGlassText(TextRenderer renderer, String text, float centerX, float y, float size, int argb) {
        float x = centerX - measureWidth(renderer, text, size) * 0.5f;
        drawLiquidGlassText(renderer, text, x, y, size, argb);
    }

    /** Truncate {@code text} with a "..." suffix until it fits within {@code maxWidth} logical pixels. */
    static String ellipsize(TextRenderer renderer, String text, float size, float maxWidth) {
        if (text == null || text.isEmpty()) return "";
        if (measureWidth(renderer, text, size) <= maxWidth) return text;
        String out = text;
        while (out.length() > 3 && measureWidth(renderer, out + "...", size) > maxWidth) {
            out = out.substring(0, out.length() - 1);
        }
        return out + "...";
    }

    /** Resolve the Inter Bold or Inter Medium {@link TextRenderer} for main-menu text, with a fallback chain. */
    static TextRenderer interBold(TextRenderer fallback) { return Fonts.renderer("Inter", FontInfo.Type.Bold, fallback); }
    static TextRenderer interMedium(TextRenderer fallback) { return Fonts.renderer("InterMedium", FontInfo.Type.Regular, fallback); }
    static TextRenderer iconFont(TextRenderer fallback) { return Fonts.renderer("MainMenuIcons", FontInfo.Type.Regular, fallback); }

    /** Recompute the fixed logical viewport from the framebuffer size, matching {@link HudScale}. */
    static float virtualWidth(int fbw, int fbh) { return Math.max(1f, HudScale.virtualWidth(fbw, fbh)); }
    static float virtualHeight(int fbw, int fbh) { return Math.max(1f, HudScale.virtualHeight(fbw, fbh)); }
}
