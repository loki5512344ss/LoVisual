/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.util.tooltip;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;

/**
 * Hover tooltip for a settings row: renders {@link Setting#getDescription()}
 * (the {@code <key>.desc} i18n entry) as a wrapped bubble below the cursor.
 * Silent when the setting has no description key.
 */
public final class SettingTooltip {
    private static final float MAX_TEXT_W = 170f;
    private static final float TEXT_SIZE = 7f;
    private static final float PAD = 4f;
    private static final float OFFSET = 12f;

    private SettingTooltip() {
    }

    /** @param scale ClickGUI ui scale; @param anchorBottom bottom of the hovered row (tooltip flips up when out of screen). */
    public static void render(Setting setting, float mx, float my, float anchorBottom, float scale, SettingsGuiPalette palette) {
        if (setting == null || scale <= 0f) return;
        String description = setting.getDescription();
        if (description == null || description.isBlank()) return;

        TextRenderer font = ClickGuiRenderer.getInterRegular();
        float size = TEXT_SIZE * scale;
        float maxWidth = MAX_TEXT_W * scale;
        java.util.List<String> lines = wrap(font, description, maxWidth, size);
        if (lines.isEmpty()) return;

        float lineH = ClickGuiRenderer.textHeight(font, size);
        float boxW = maxWidth + PAD * 2f * scale;
        for (String line : lines) {
            boxW = Math.max(boxW, ClickGuiRenderer.textWidth(font, line, size) + PAD * 2f * scale);
        }
        float boxH = lines.size() * lineH + PAD * 2f * scale;

        float x = clampX(mx + OFFSET * scale, boxW);
        float y = anchorBottom + 3f * scale;
        float screenH = screenHeight();
        if (y + boxH > screenH - 4f) {
            y = Math.max(4f, my - boxH - OFFSET * scale);
        }

        int bg = LayoutRender2D.alpha(palette.menuShadow(), 0.92f);
        int stroke = LayoutRender2D.alpha(palette.panelStroke(), 0.9f);
        float radius = 3.5f * scale;
        LayoutRender2D.roundedQuad(x, y, boxW, boxH, radius, bg, bg, bg, bg);
        LayoutRender2D.roundedStrokeQuad(x, y, boxW, boxH, radius, 0.5f * scale,
                stroke, stroke, stroke, stroke);

        int textColor = palette.moduleDescriptionText();
        float ty = y + PAD * scale;
        for (String line : lines) {
            ClickGuiRenderer.drawText(font, line, x + PAD * scale, ty, size, textColor, false);
            ty += lineH;
        }
    }

    private static java.util.List<String> wrap(TextRenderer font, String text, float maxWidth, float size) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (word.isEmpty()) continue;
            String candidate = line.isEmpty() ? word : line + " " + word;
            if (!line.isEmpty() && ClickGuiRenderer.textWidth(font, candidate, size) > maxWidth) {
                lines.add(line.toString());
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (!line.isEmpty()) lines.add(line.toString());
        return lines;
    }

    private static float screenHeight() {
        Minecraft mc = Minecraft.getInstance();
        return mc != null ? mc.getWindow().getGuiScaledHeight() : 240f;
    }

    private static float clampX(float x, float boxW) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return x;
        float screenW = mc.getWindow().getGuiScaledWidth();
        return Math.min(Math.max(4f, x), Math.max(4f, screenW - boxW - 4f));
    }
}
