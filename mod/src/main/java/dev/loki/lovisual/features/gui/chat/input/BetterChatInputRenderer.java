/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.features.gui.chat.input;
import dev.loki.lovisual.mixins.accessors.chat.ChatScreenAccessor;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.util.screen.ClientScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import java.util.ArrayList;
import java.util.List;
import static dev.loki.lovisual.features.theme.Theme.theme;
import dev.loki.lovisual.features.gui.chat.messages.BetterChatSearch;
import dev.loki.lovisual.features.gui.chat.model.BetterChatModel;
import dev.loki.lovisual.features.gui.chat.render.BetterChatRenderer;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatDrawUtil;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatFontUtil;
/**
 * Input-field rendering: wrapping, caret, selection highlight, char counter and search chip.
 */
public final class BetterChatInputRenderer {
    private BetterChatInputRenderer() {
    }
    public static void renderInput(Minecraft mc, float fontSize, float boxW, float x, float inputY) {
        if (!(ClientScreen.current() instanceof ChatScreen chatScreen)) return;
        EditBox field = ((ChatScreenAccessor) chatScreen).getChatField();
        if (field == null) return;
        String fieldText = field.getValue();
        boolean searchMode = BetterChatSearch.isActive();
        String text = searchMode ? BetterChatSearch.getText() : fieldText;
        int cursor = searchMode ? text.length() : field.getCursorPosition();
        int selStart = searchMode ? cursor : BetterChatEditBoxUtil.selectionStart(field, cursor);
        int selEnd = searchMode ? cursor : BetterChatEditBoxUtil.selectionEnd(field, cursor);
        float padX = 12f;
        float padY = 7f;
        float lineHeight = fontSize + 2f;
        float iconGap = 8f;
        float searchBoxW = BetterChatRenderer.SEARCH_ICON_WIDTH;
        float searchBoxH = BetterChatRenderer.SEARCH_ICON_HEIGHT;
        float counterReserve = (!searchMode && text.length() >= BetterChatRenderer.MAX_MESSAGE_CHARS - 20) ? 34f : 0f;
        float modeReserve = (searchMode || BetterChatSearch.hasQuery()) ? 74f : 0f;
        float rightReserve = counterReserve + modeReserve;
        if (!searchMode && text.length() > BetterChatRenderer.MAX_MESSAGE_CHARS) {
            text = text.substring(0, BetterChatRenderer.MAX_MESSAGE_CHARS);
            field.setValue(text);
            BetterChatEditBoxUtil.setCursorSafe(field, text.length());
        }
        int remaining = searchMode ? Integer.MAX_VALUE : BetterChatRenderer.MAX_MESSAGE_CHARS - text.length();
        float maxLineWidth = Math.max(32f, boxW - padX * 2f - searchBoxW - iconGap - rightReserve);
        List<BetterChatModel.InputLine> lines = wrapInput(text, fontSize, maxLineWidth);
        if (lines.isEmpty()) {
            lines = List.of(new BetterChatModel.InputLine(0, 0, 0f));
        }
        float contentHeight = lines.size() * lineHeight;
        float h = Math.max(36f, contentHeight + padY * 2f);
        BetterChatRenderer.inputBoxX = x;
        BetterChatRenderer.inputBoxY = inputY;
        BetterChatRenderer.inputBoxW = boxW;
        BetterChatRenderer.inputBoxH = h;
        BetterChatRenderer.hasInputBox = true;
        BetterChatDrawUtil.drawGlassPill(x, inputY, boxW, h, Math.min(BetterChatRenderer.INPUT_RADIUS, h * 0.5f), 0.92f, false);
        float searchX = x + padX;
        float searchY = inputY + (h - searchBoxH) * 0.5f;
        renderSearchIcon(searchX, searchY, searchBoxW, searchBoxH, BetterChatSearch.isActive(), BetterChatSearch.hasQuery());
        BetterChatRenderer.searchRectX = searchX;
        BetterChatRenderer.searchRectY = searchY;
        BetterChatRenderer.searchRectW = searchBoxW;
        BetterChatRenderer.searchRectH = searchBoxH;
        BetterChatRenderer.searchHit = true;
        float textX = searchX + searchBoxW + iconGap;
        float textClipW = Math.max(20f, x + boxW - padX - textX - rightReserve);
        TextRenderer tr = BetterChatFontUtil.getIosevkaRegular();
        float textScale = BetterChatFontUtil.scaleForSize(fontSize);
        boolean clipped = ScissorFunction.pushRaw(textX, inputY, textClipW, h);
        if (tr != null) {
            tr.begin(textScale, false, false);
            float textH = (float) tr.getHeight(false);
            tr.end();
            float textYOffset = (lineHeight - textH) * 0.5f;
            float lineY = inputY + padY + textYOffset;
            if (text.isEmpty()) {
                BetterChatFontUtil.drawText(tr, searchMode ? "Search chat..." : "Message...", textX, lineY, fontSize, theme().textMuted(), false);
            } else {
                for (BetterChatModel.InputLine line : lines) {
                    String slice = BetterChatFontUtil.safeSub(text, line.start(), line.end());
                    BetterChatFontUtil.drawText(tr, slice, textX, lineY, fontSize, theme().textPrimary(), true);
                    lineY += lineHeight;
                }
            }
        }
        int min = Math.min(selStart, selEnd);
        int max = Math.max(selStart, selEnd);
        if (max > min) {
            for (int i = 0; i < lines.size(); i++) {
                BetterChatModel.InputLine line = lines.get(i);
                int lineStart = line.start();
                int lineEnd = line.end();
                int selA = Math.max(min, lineStart);
                int selB = Math.min(max, lineEnd);
                if (selB > selA) {
                    float selX = textX + BetterChatFontUtil.textWidth(BetterChatFontUtil.getIosevkaRegular(), BetterChatFontUtil.safeSub(text, lineStart, selA), fontSize);
                    float selW = BetterChatFontUtil.textWidth(BetterChatFontUtil.getIosevkaRegular(), BetterChatFontUtil.safeSub(text, selA, selB), fontSize);
                    float sy = inputY + padY + i * lineHeight;
                    BetterChatDrawUtil.drawRoundedRect(selX, sy, selW, lineHeight, 3.5f, BetterChatDrawUtil.withAlpha(theme().accent(), 0x4A));
                }
            }
        }
        int caretLine = 0;
        for (int i = 0; i < lines.size(); i++) {
            BetterChatModel.InputLine line = lines.get(i);
            if (cursor >= line.start() && cursor <= line.end()) {
                caretLine = i;
                break;
            }
        }
        float caretX = textX + widthTo(text, lines, cursor, fontSize);
        float caretY = inputY + padY + caretLine * lineHeight;
        boolean blink = (System.currentTimeMillis() / 500L) % 2 == 0;
        if (blink) {
            BetterChatDrawUtil.drawRoundedRect(caretX, caretY + 1f, 1.35f, Math.max(8f, lineHeight - 2f), 0.8f, BetterChatDrawUtil.withAlpha(theme().textPrimary(), 0xD8));
        }
        if (clipped) ScissorFunction.pop();
        if (BetterChatSearch.isActive() || BetterChatSearch.hasQuery()) {
            String q = BetterChatSearch.isActive() ? "Search" : BetterChatSearch.getText();
            float chipFont = Math.max(10f, fontSize * 0.72f);
            String chipText = BetterChatFontUtil.fitText(BetterChatFontUtil.getInterRegular(), q, chipFont, Math.max(36f, boxW * 0.28f));
            float chipW = BetterChatFontUtil.textWidth(BetterChatFontUtil.getInterRegular(), chipText, chipFont) + 14f;
            float chipH = Math.min(22f, h - 8f);
            float chipX = x + boxW - padX - chipW - counterReserve;
            float chipY = inputY + (h - chipH) * 0.5f;
            BetterChatDrawUtil.drawRoundedRect(chipX, chipY, chipW, chipH, chipH * 0.5f, BetterChatDrawUtil.withAlpha(theme().accentSoft(), BetterChatSearch.isActive() ? 0x66 : 0x40));
            BetterChatFontUtil.drawText(BetterChatFontUtil.getInterRegular(), chipText, chipX + 7f, chipY + (chipH - BetterChatFontUtil.textHeight(BetterChatFontUtil.getInterRegular(), chipFont)) * 0.5f, chipFont,
                    BetterChatSearch.hasQuery() ? theme().textPrimary() : theme().textMuted(), false);
        }
        if (!searchMode && remaining <= 20) {
            String remStr = String.valueOf(Math.max(0, remaining));
            float remSize = fontSize * 0.74f;
            int remColor = remaining <= 5 ? BetterChatDrawUtil.withAlpha(theme().accent(), 0xFF) : BetterChatDrawUtil.withAlpha(theme().textMuted(), 0xCC);
            BetterChatFontUtil.drawText(BetterChatFontUtil.getIosevkaRegular(), remStr, searchX + 2.0f,
                    inputY + h + 3.0f,
                    remSize, remColor, false);
        }
    }
    private static List<BetterChatModel.InputLine> wrapInput(String text, float fontSize, float maxWidth) {
        List<BetterChatModel.InputLine> lines = new ArrayList<>();
        int start = 0;
        float width = 0f;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            int cpLen = Character.charCount(cp);
            String glyphText = new String(Character.toChars(cp));
            float cw = BetterChatFontUtil.textWidth(BetterChatFontUtil.getIosevkaRegular(), glyphText, fontSize);
            if (width + cw > maxWidth && width > 0f) {
                lines.add(new BetterChatModel.InputLine(start, i, width));
                start = i;
                width = 0f;
            }
            width += cw;
            i += cpLen;
        }
        lines.add(new BetterChatModel.InputLine(start, text.length(), width));
        return lines;
    }
    private static float widthTo(String text, List<BetterChatModel.InputLine> lines, int cursorIndex, float fontSize) {
        if (lines.isEmpty()) return 0f;
        for (BetterChatModel.InputLine line : lines) {
            if (cursorIndex >= line.start() && cursorIndex <= line.end()) {
                return BetterChatFontUtil.textWidth(BetterChatFontUtil.getIosevkaRegular(), BetterChatFontUtil.safeSub(text, line.start(), cursorIndex), fontSize);
            }
        }
        BetterChatModel.InputLine last = lines.getLast();
        return BetterChatFontUtil.textWidth(BetterChatFontUtil.getIosevkaRegular(), BetterChatFontUtil.safeSub(text, last.start(), last.end()), fontSize);
    }
    static void renderSearchIcon(float x, float y, float w, float h, boolean active, boolean hasQuery) {
        boolean hover = BetterChatRenderer.lastMouseValid && BetterChatRenderer.lastMouseFx >= x && BetterChatRenderer.lastMouseFx <= x + w && BetterChatRenderer.lastMouseFy >= y && BetterChatRenderer.lastMouseFy <= y + h;
        float radius = h * 0.5f;
        int bg = active
                ? BetterChatDrawUtil.withAlpha(theme().accent(), 0x8E)
                : (hasQuery ? BetterChatDrawUtil.withAlpha(theme().accentSoft(), 0x8A) : BetterChatDrawUtil.withAlpha(theme().surface(), hover ? 0xC0 : 0xA2));
        int tint = BetterChatDrawUtil.withAlpha(theme().accent(), active ? 0x30 : (hasQuery ? 0x24 : 0x18));
        int strokeA = active ? BetterChatDrawUtil.withAlpha(theme().accent(), 0xC8) : BetterChatDrawUtil.withAlpha(theme().textPrimary(), hover ? 0x38 : 0x24);
        int strokeB = BetterChatDrawUtil.withAlpha(theme().accentSoft(), active ? 0x86 : (hasQuery ? 0x66 : 0x40));
        BetterChatDrawUtil.drawRoundedRect(x, y, w, h, radius, bg);
        BetterChatDrawUtil.drawRoundedRect(x + 1f, y + 1f, w - 2f, h - 2f, Math.max(0f, radius - 1f), tint);
        BetterChatDrawUtil.drawRoundedRectStrokeGradient(x, y, w, h, radius, 0.7f, strokeA, strokeB);
        TextRenderer icons = Fonts.renderer("Icons", FontInfo.Type.Regular, BetterChatFontUtil.getInterRegular());
        String icon = "s";
        float iconSize = h * 0.50f;
        float iconW = BetterChatFontUtil.textWidth(icons, icon, iconSize);
        float iconH = BetterChatFontUtil.textHeight(icons, iconSize);
        int iconColor = active || hasQuery ? theme().textPrimary() : BetterChatDrawUtil.withAlpha(theme().textPrimary(), hover ? 0xD8 : 0xB8);
        BetterChatFontUtil.drawText(icons, icon, x + (w - iconW) * 0.5f, y + (h - iconH) * 0.5f - h * 0.015f, iconSize, iconColor, false);
    }
}
