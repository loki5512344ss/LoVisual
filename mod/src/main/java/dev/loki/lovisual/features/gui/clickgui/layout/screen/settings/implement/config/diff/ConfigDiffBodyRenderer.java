package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config.diff;

import dev.loki.lovisual.config.profile.diff.ConfigProfileDiff;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;

/**
 * Body of the diff panel: the empty-state messages and the per-setting change
 * lines, plus the height math the scroller needs before painting. Stateless —
 * the panel passes its fade value in on every call.
 */
public final class ConfigDiffBodyRenderer {
    private ConfigDiffBodyRenderer() {
    }

    public static void render(float x,
                              float y,
                              float w,
                              float clipY,
                              float clipH,
                              ConfigProfileMeta selected,
                              ConfigProfileDiff diff,
                              String error,
                              float scale,
                              SettingsGuiPalette palette,
                              float openAnim) {
        if (error != null && !error.isBlank()) {
            drawLine(ClickGuiI18n.tr("clickgui.settings.config.diff.error", "Diff error: %s", error), x, y, 7.2f * scale, palette.panelMuted(), openAnim);
            return;
        }
        if (selected == null) {
            drawLine(ClickGuiI18n.tr("clickgui.settings.config.diff.select_config", "Select a config and press diff."), x, y, 7.2f * scale, palette.panelMuted(), openAnim);
            return;
        }
        if (diff == null) {
            drawLine(selected.name(), x, y, 7.2f * scale, palette.panelText(), openAnim);
            drawLine(ClickGuiI18n.tr("clickgui.settings.config.diff.not_loaded", "Diff is not loaded."), x, y + 10f * scale, 7.2f * scale, palette.panelMuted(), openAnim);
            return;
        }
        if (diff.isEmpty()) {
            drawLine(selected.name(), x, y, 7.2f * scale, palette.panelText(), openAnim);
            drawLine(ClickGuiI18n.tr("clickgui.settings.config.diff.no_differences", "No differences."), x, y + 11f * scale, 7.2f * scale, palette.panelMuted(), openAnim);
            return;
        }

        float cy = y;
        drawVisibleLine(ClickGuiRenderer.fitText(ClickGuiRenderer.getInterMedium(), selected.name(), 7.4f * scale, w), x, cy, 7.4f * scale, palette.panelText(), clipY, clipH, scale, openAnim);
        cy += 11f * scale;
        drawVisibleLine(ClickGuiI18n.tr("clickgui.settings.config.diff.changed", "Changes: %s", diff.valueChangeCount()), x, cy, 7f * scale, palette.panelMuted(), clipY, clipH, scale, openAnim);
        cy += 13f * scale;

        for (ConfigProfileDiff.Entry entry : diff.entries()) {
            String owner = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterMedium(), entry.displayName(), 7.4f * scale, w);
            drawVisibleLine(owner, x, cy, 7.4f * scale, palette.panelText(), clipY, clipH, scale, openAnim);
            cy += 9.5f * scale;

            for (ConfigProfileDiff.Value value : entry.values()) {
                String text = value.key() + ": " + stringify(value.currentValue()) + " -> " + stringify(value.selectedValue());
                text = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterRegular(), text, 6.7f * scale, w - 5f * scale);
                drawVisibleLine(text, x + 5f * scale, cy, 6.7f * scale, palette.panelMuted(), clipY, clipH, scale, openAnim);
                cy += 8.5f * scale;
            }
            cy += 4f * scale;
        }
    }

    public static float contentHeight(ConfigProfileMeta selected, ConfigProfileDiff diff, String error, float scale) {
        if (error != null && !error.isBlank()) return 11f * scale;
        if (selected == null) return 11f * scale;
        if (diff == null || diff.isEmpty()) return 23f * scale;

        float h = 24f * scale;
        for (ConfigProfileDiff.Entry entry : diff.entries()) {
            h += 9.5f * scale;
            h += entry.values().size() * 8.5f * scale;
            h += 4f * scale;
        }
        return h;
    }

    private static String stringify(Object value) {
        if (value == null) return "null";
        String text = String.valueOf(value);
        if (text.length() > 34) text = text.substring(0, 31) + "...";
        return text;
    }

    private static void drawVisibleLine(String text,
                                        float x,
                                        float y,
                                        float size,
                                        int color,
                                        float clipY,
                                        float clipH,
                                        float scale,
                                        float openAnim) {
        if (y + 9f * scale < clipY || y > clipY + clipH) return;
        drawLine(text, x, y, size, color, openAnim);
    }

    private static void drawLine(String text, float x, float y, float size, int color, float openAnim) {
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), text, x, y, size, LayoutRender2D.alpha(color, openAnim), false);
    }
}
