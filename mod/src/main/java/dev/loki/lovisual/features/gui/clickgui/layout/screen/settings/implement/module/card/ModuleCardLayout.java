package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

import java.util.List;

final class ModuleCardLayout {
    private final ModuleCardState state;

    ModuleCardLayout(ModuleCardState state) {
        this.state = state;
    }

    float descriptionHeight(String description, float w, float scale) {
        float maxWidth = w - 25f * scale;
        float currentX = 0f;
        int lineCount = 1;
        for (String word : description.split(" ")) {
            float wordW = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), word + " ", 10f * scale);
            if (currentX + wordW > maxWidth) {
                lineCount++;
                currentX = 0f;
            }
            currentX += wordW;
        }
        float lineH = ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterRegular(), 10f * scale);
        float compactStep = Math.max(scale, lineH - 5.75f * scale);
        return Math.max(0f, lineH - 4f * scale + (lineCount - 1) * compactStep);
    }

    int getComponentHeight(ModuleComponent.CardEntry entry, float w, float scale) {
        return Math.round(45f * scale + descriptionHeight(normalizeDescription(entry.description()), w, scale));
    }

    int[] calculateOffsets(List<ModuleComponent.CardEntry> entries, float scale) {
        int[] offsets = new int[2];
        int column = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            int componentHeight = Math.round(getComponentHeight(entries.get(i), 182f * scale, scale) + 9f * scale);
            offsets[column] += componentHeight;
            column = (column + 1) % 2;
        }
        return offsets;
    }

    void updateScrollbarMetrics(float menuX, float menuY, float menuH, int clamped, float scale) {
        state.scrollbarVisible = clamped > 0;
        state.scrollbarMaxScroll = Math.max(0f, clamped);
        if (!state.scrollbarVisible) {
            state.draggingScrollbar = false;
            state.scrollbarX = 0f;
            state.scrollbarY = 0f;
            state.scrollbarW = 0f;
            state.scrollbarH = 0f;
            state.scrollbarThumbY = 0f;
            state.scrollbarThumbH = 0f;
            return;
        }
        float offsetY = 14f * scale;
        state.scrollbarW = 4f * scale;
        state.scrollbarX = menuX + 400f * scale - 35f * scale - state.scrollbarW + 50f * scale;
        state.scrollbarY = menuY + offsetY + 22f * scale;
        state.scrollbarH = menuH - offsetY * 2f - 14f * scale;
        float contentHeight = state.scrollbarMaxScroll;
        float viewHeight = menuH - offsetY * 2f;
        state.scrollbarThumbH = Math.max(20f * scale, viewHeight * (viewHeight / (contentHeight + viewHeight)));
        float scrollRatio = contentHeight <= 0f ? 0f : (-state.smoothedScroll / contentHeight);
        state.scrollbarThumbY = state.scrollbarY + (state.scrollbarH - state.scrollbarThumbH)
                * AnimationUtility.clamp(scrollRatio, 0f, 1f);
    }

    boolean isScrollbarHovered(float mx, float my) {
        if (!state.scrollbarVisible) return false;
        float pad = 4f;
        return ClickGuiMath.insideRect(mx, my, state.scrollbarX - pad, state.scrollbarY - pad,
                state.scrollbarW + pad * 2f, state.scrollbarH + pad * 2f);
    }

    void scrollToMouse(float my) {
        if (!state.scrollbarVisible || state.scrollbarMaxScroll <= 0f) return;
        float span = state.scrollbarH - state.scrollbarThumbH;
        if (span <= 0.5f) return;
        float thumbTop = AnimationUtility.clamp(my - state.scrollbarDragOffset, state.scrollbarY, state.scrollbarY + span);
        float ratio = (thumbTop - state.scrollbarY) / span;
        state.scroll = -state.scrollbarMaxScroll * AnimationUtility.clamp(ratio, 0f, 1f);
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n
                    .tr("clickgui.settings.module.description_missing", "Module description is missing.");
        }
        return description;
    }

    void drawDescription(String description, float x, float y, float w, float scale, SettingsGuiPalette palette) {
        float maxWidth = w - 25f * scale;
        float currentX = x + 10f * scale;
        float currentY = y + 19f * scale;
        String[] words = description.split(" ");
        StringBuilder line = new StringBuilder();
        int currentLine = 1;
        float textSize = 10f * scale;
        TextRenderer icons = Fonts.renderer("Icons", FontInfo.Type.Regular, ClickGuiRenderer.getInterRegular());
        int iconColor = palette.moduleDescriptionIcon();
        int textColor = palette.moduleDescriptionText();
        for (String word : words) {
            float wordWidth = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), word + " ", textSize);
            if (currentX + wordWidth > x + maxWidth) {
                if (currentLine == 1) {
                    ClickGuiRenderer.drawText(icons, "J", x + 6.5f * scale, currentY + 0.75f * scale, 9f * scale, iconColor, false);
                    ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), line.toString(), x + 15f * scale, currentY, textSize, textColor, false);
                } else {
                    ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), line.toString(), x + 5f * scale, currentY, textSize, textColor, false);
                }
                line = new StringBuilder();
                currentY += ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterRegular(), textSize) - 5.75f * scale;
                currentX = x + 10f * scale;
                currentLine++;
            }
            line.append(word).append(" ");
            currentX += wordWidth;
        }
        if (!line.isEmpty()) {
            if (currentLine == 1) {
                ClickGuiRenderer.drawText(icons, "J", x + 6.5f * scale, currentY + 0.75f * scale, 9f * scale, iconColor, false);
                ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), line.toString(), x + 15f * scale, currentY, textSize, textColor, false);
            } else {
                ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), line.toString(), x + 7f * scale, currentY, textSize, textColor, false);
            }
        }
    }

    BindRect drawBind(ModuleComponent.CardEntry entry, float x, float y, float w, float descHeight, float scale, SettingsGuiPalette palette) {
        String label = entry.bindLabel();
        if (label == null || label.isBlank()) return new BindRect(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f);
        float labelW = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), label, 12f * scale);
        float bindX = x + w - 37.5f * scale - labelW;
        float bindY = y + descHeight + 29.75f * scale;
        float bindW = labelW + 6f * scale;
        float bindH = 10f * scale;
        float hitY = y + descHeight + 32.5f * scale;
        float hitH = 9f * scale;
        LayoutRender2D.roundedQuad(bindX + 0.25f * scale, bindY, bindW, bindH, 3f * scale,
                palette.moduleBindBg0(), palette.moduleBindBg1(), palette.moduleBindBg2(), palette.moduleBindBg3());
        LayoutRender2D.roundedStroke(bindX + 0.25f * scale, bindY, bindW, bindH, 3f * scale, 0.5f * scale, palette.moduleBindStroke());
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), label, x + w - 34.5f * scale - labelW, y + descHeight + 34f * scale, 12f * scale, palette.moduleBindText(), false);
        return new BindRect(bindX, bindY, bindW, bindH, bindX, hitY, bindW, hitH);
    }

    void drawNoSettingsHint(float x, float y, float descHeight, float scale, SettingsGuiPalette palette) {
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(),
                ClickGuiI18n.tr("clickgui.settings.module.no_settings", "No settings"),
                x + 10f * scale, y + descHeight + 33.5f * scale, 9f * scale, palette.moduleNoSettingsText(), false);
    }
}
