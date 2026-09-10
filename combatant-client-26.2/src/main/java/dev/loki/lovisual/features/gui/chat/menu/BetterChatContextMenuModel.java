/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.chat.menu;

import java.util.Collections;
import java.util.List;

import static dev.loki.lovisual.features.theme.Theme.theme;
import dev.loki.lovisual.features.gui.chat.render.BetterChatRenderer;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatDrawUtil;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatFontUtil;

/**
 * Right-click context menu record + entry record, extracted from the renderer facade.
 * Rendering leans on {@link BetterChatDrawUtil} and {@link BetterChatFontUtil}.
 */
public final class BetterChatContextMenuModel {
    private BetterChatContextMenuModel() {
    }

    public record ContextMenu(boolean open, float x, float y, List<MenuEntry> entries) {
        private static final float MENU_PAD_X = 8f;
        private static final float MENU_PAD_Y = 7f;
        private static final float MENU_RADIUS = 9f;

        public static ContextMenu closed() {
            return new ContextMenu(false, 0, 0, Collections.emptyList());
        }

        static ContextMenu open(float x, float y, List<MenuEntry> entries) {
            return new ContextMenu(true, x, y, entries);
        }

        private static float itemHeight(float fontSize) {
            return Math.max(20f, fontSize + 7f);
        }

        public boolean contains(double mx, double my) {
            if (!open) return false;
            float fontSize = BetterChatRenderer.lastFontSizeForUi * 0.92f;
            return mx >= x && mx <= x + width(fontSize) && my >= y && my <= y + height(fontSize);
        }

        public MenuEntry pick(double my) {
            if (!open) return null;
            float fontSize = BetterChatRenderer.lastFontSizeForUi * 0.92f;
            float itemH = itemHeight(fontSize);
            float cy = y + MENU_PAD_Y;
            for (int i = 0; i < entries.size(); i++) {
                float top = cy + i * itemH;
                float bottom = top + itemH;
                if (my >= top && my <= bottom) {
                    return entries.get(i);
                }
            }
            return null;
        }

        public void render(float mouseX, float mouseY, float fontSize) {
            if (!open || entries.isEmpty()) return;
            float fs = Math.max(12f, fontSize * 0.92f);
            float itemH = itemHeight(fs);
            float w = width(fs);
            float h = height(fs);

            BetterChatDrawUtil.drawGlassPill(x, y, w, h, MENU_RADIUS, 0.94f, true);

            float accentX = x + 5f;
            BetterChatDrawUtil.drawRoundedRect(accentX, y + MENU_PAD_Y + 2f, 2f, h - MENU_PAD_Y * 2f - 4f, 1f, BetterChatDrawUtil.withAlpha(theme().accent(), 0x72));

            float cy = y + MENU_PAD_Y;
            for (MenuEntry entry : entries) {
                boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= cy && mouseY <= cy + itemH;
                if (hover) {
                    BetterChatDrawUtil.drawRoundedRect(x + 8f, cy + 1f, w - 16f, itemH - 2f, itemH * 0.5f, BetterChatDrawUtil.withAlpha(theme().surfaceHover(), 0xAA));
                    BetterChatDrawUtil.drawRoundedRectStrokeGradient(
                            x + 8f, cy + 1f, w - 16f, itemH - 2f, itemH * 0.5f,
                            0.55f,
                            BetterChatDrawUtil.withAlpha(theme().accent(), 0x66),
                            BetterChatDrawUtil.withAlpha(theme().accentSoft(), 0x38)
                    );
                }

                float dot = 3.2f;
                int dotColor = hover ? BetterChatDrawUtil.withAlpha(theme().accent(), 0xE0) : BetterChatDrawUtil.withAlpha(theme().textMuted(), 0x70);
                BetterChatDrawUtil.drawRoundedRect(x + 14f, cy + (itemH - dot) * 0.5f, dot, dot, dot * 0.5f, dotColor);

                float textY = cy + (itemH - BetterChatFontUtil.textHeight(BetterChatFontUtil.getIosevkaRegular(), fs)) * 0.5f;
                int textColor = hover ? theme().textPrimary() : BetterChatDrawUtil.withAlpha(theme().textPrimary(), 0xE4);
                BetterChatFontUtil.drawText(BetterChatFontUtil.getIosevkaRegular(), entry.label(), x + 24f, textY, fs, textColor, true);
                cy += itemH;
            }
        }

        private float width(float fontSize) {
            float maxText = 0f;
            for (MenuEntry e : entries) {
                maxText = Math.max(maxText, BetterChatFontUtil.textWidth(BetterChatFontUtil.getIosevkaRegular(), e.label(), fontSize));
            }
            return Math.max(168f, maxText + MENU_PAD_X * 2f + 34f);
        }

        private float height(float fontSize) {
            return MENU_PAD_Y * 2f + entries.size() * itemHeight(fontSize);
        }

        public record MenuEntry(String label, Runnable action) {
        }
    }
}
