
package dev.loki.lovisual.features.gui.chat.suggestions;
import dev.loki.lovisual.features.command.util.CommandPrefix;
import dev.loki.lovisual.mixins.accessors.chat.ChatScreenAccessor;
import dev.loki.lovisual.render.helpers.util.SystemCursor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.Mth;
import java.util.List;
import static dev.loki.lovisual.features.theme.Theme.theme;
import dev.loki.lovisual.features.gui.chat.input.BetterChatEditBoxUtil;
import dev.loki.lovisual.features.gui.chat.render.BetterChatRenderer;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatDrawUtil;
import dev.loki.lovisual.features.gui.chat.text.util.BetterChatFontUtil;
/**
 * Draws the autocomplete suggestion popup above the input field each frame.
 */
public final class BetterChatSuggestionRenderer {
    private BetterChatSuggestionRenderer() {
    }
    public static void renderSuggestions() {
        BetterChatRenderer.suggestActive = false;
        BetterChatRenderer.suggestHasScrollbar = false;
        if (BetterChatRenderer.storedScreen == null) return;
        EditBox field = ((ChatScreenAccessor) BetterChatRenderer.storedScreen).getChatField();
        String text = field != null ? field.getValue() : null;
        if (field == null || text == null || text.isEmpty()) {
            BetterChatRenderer.storedScreen = null;
            BetterChatSuggestionState.resetSuggestionTracking();
            return;
        }
        int cursor = field.getCursorPosition();
        int selStart = BetterChatEditBoxUtil.selectionStart(field, cursor);
        int selEnd = BetterChatEditBoxUtil.selectionEnd(field, cursor);
        char prefix = BetterChatSuggestionState.commandPrefix(text);
        if (prefix == 0) {
            BetterChatRenderer.storedScreen = null;
            BetterChatSuggestionState.resetSuggestionTracking();
            return;
        }
        boolean inputChanged = BetterChatSuggestionState.refreshSuggestionsIfNeeded(text, cursor, selStart, selEnd);
        boolean windowChanged = false;
        if (prefix == '/') {
            windowChanged = CommandSuggestorBridge.peekWindow(BetterChatRenderer.storedScreen) != CommandSuggestorBridge.lastWindow();
        }
        if (inputChanged || windowChanged || BetterChatRenderer.lastSnapshot == null) {
            if (prefix == CommandPrefix.CHAR || prefix == CommandPrefix.LEGACY) {
                BetterChatRenderer.lastSnapshot = BetterChatSuggestionState.buildCustomSnapshot(text, cursor);
            } else {
                BetterChatRenderer.lastSnapshot = CommandSuggestorBridge.snapshot(BetterChatRenderer.storedScreen);
            }
        }
        CommandSuggestorBridge.SuggestionSnapshot snap = BetterChatRenderer.lastSnapshot;
        if (snap == null) {
            BetterChatRenderer.storedScreen = null;
            BetterChatSuggestionState.resetSuggestionTracking();
            return;
        }
        if (prefix == CommandPrefix.CHAR || prefix == CommandPrefix.LEGACY) {
            BetterChatRenderer.suggestWindow = null;
        }
        int hash = snap.texts().hashCode() ^ snap.selection() ^ snap.startIndex();
        boolean snapshotChanged = hash != BetterChatRenderer.lastSuggestionHash;
        if (snapshotChanged) {
            BetterChatRenderer.lastSuggestionHash = hash;
        }
        BetterChatRenderer.suggestEntries = snap.suggestions();
        int total = snap.texts().size();
        int visible = Math.min(5, Math.max(1, total - snap.startIndex()));
        int scrollable = Math.max(0, total - visible);
        if (snapshotChanged) {
            BetterChatRenderer.suggestCurrentStart = Mth.clamp(snap.startIndex(), 0, scrollable);
            BetterChatRenderer.suggestCurrentSelection = Mth.clamp(snap.selection(), 0, Math.max(0, total - 1));
        }
        if (BetterChatRenderer.suggestCurrentStart < 0) {
            BetterChatRenderer.suggestCurrentStart = Mth.clamp(snap.startIndex(), 0, scrollable);
        } else {
            BetterChatRenderer.suggestCurrentStart = Mth.clamp(BetterChatRenderer.suggestCurrentStart, 0, scrollable);
        }
        if (BetterChatRenderer.suggestCurrentSelection < 0) {
            BetterChatRenderer.suggestCurrentSelection = Mth.clamp(snap.selection(), 0, Math.max(0, total - 1));
        } else {
            BetterChatRenderer.suggestCurrentSelection = Mth.clamp(BetterChatRenderer.suggestCurrentSelection, 0, Math.max(0, total - 1));
        }
        BetterChatSuggestionState.applySuggestionWindowState(BetterChatRenderer.suggestCurrentStart, BetterChatRenderer.suggestCurrentSelection);
        float baseFont = Math.max(10f, BetterChatRenderer.lastFontSizeForUi * 0.95f);
        float itemH = baseFont + 6f;
        ScreenRect rect = computeBox(snap, baseFont, visible, itemH);
        float x = rect.x;
        float w = rect.w;
        float h = rect.h;
        float y = rect.y;
        if (w <= 2f || h <= 2f || snap.texts().isEmpty()) {
            BetterChatRenderer.storedScreen = null;
            return;
        }
        boolean hasScrollbar = total > visible;
        float scrollbarReserve = hasScrollbar ? 13f : 0f;
        float maxTextWidth = textWidthRange(snap.texts(), snap.startIndex(), visible, baseFont);
        w = Math.max(w, maxTextWidth + 18f + scrollbarReserve);
        BetterChatDrawUtil.drawGlassPill(x, y, w, h, 7f, 0.82f, true);
        float trackW = hasScrollbar ? 4.8f : 0f;
        float trackX = x + w - trackW - 5f;
        float trackY = y + 3.5f;
        float trackH = h - 7f;
        float thumbH = hasScrollbar ? Math.max(12f, trackH * ((float) visible / (float) total)) : 0f;
        float thumbY = hasScrollbar ? trackY + (trackH - thumbH) * ((float) BetterChatRenderer.suggestCurrentStart / (float) (Math.max(1, total - visible))) : trackY;
        float rowX = x + 3f;
        float rowY = y + 3f;
        float rowW = Math.max(0f, (hasScrollbar ? trackX - 3f : x + w - 3f) - rowX);
        float rowH = visible * itemH;
        for (int i = 0; i < visible; i++) {
            int idx = BetterChatRenderer.suggestCurrentStart + i;
            if (idx < 0 || idx >= snap.texts().size()) break;
            float top = rowY + i * itemH;
            boolean hoverRow = BetterChatRenderer.lastMouseValid &&
                    BetterChatRenderer.lastMouseFx >= rowX && BetterChatRenderer.lastMouseFx <= rowX + rowW &&
                    BetterChatRenderer.lastMouseFy >= top && BetterChatRenderer.lastMouseFy <= top + itemH;
            if (hoverRow) {
                BetterChatDrawUtil.drawLiquidHoverPill(rowX, top + 0.6f, rowW, itemH - 1.2f, 4.5f, 0.46f);
            }
            float textHh = BetterChatFontUtil.textHeight(BetterChatFontUtil.getIosevkaRegular(), baseFont);
            float textY = top + (itemH - textHh) * 0.5f;
            BetterChatFontUtil.drawText(BetterChatFontUtil.getIosevkaRegular(), snap.texts().get(idx), rowX + 4f, textY, baseFont, theme().textPrimary(), true);
        }
        if (hasScrollbar) {
            boolean hotScroll = BetterChatRenderer.suggestDraggingScrollbar || (BetterChatRenderer.lastMouseValid
                    && BetterChatRenderer.lastMouseFx >= trackX - 4f && BetterChatRenderer.lastMouseFx <= trackX + trackW + 4f
                    && BetterChatRenderer.lastMouseFy >= trackY - 4f && BetterChatRenderer.lastMouseFy <= trackY + trackH + 4f);
            if (hotScroll) {
                SystemCursor.set(SystemCursor.CursorType.SCROLL);
            }
            BetterChatDrawUtil.drawScrollbarGlass(trackX, trackY, trackW, trackH, thumbY, thumbH, hotScroll, hotScroll ? 0xB0 : 0x88, hotScroll ? 0xFF : 0xD8);
        }
        BetterChatRenderer.suggestActive = true;
        BetterChatRenderer.suggestX = x;
        BetterChatRenderer.suggestY = y;
        BetterChatRenderer.suggestW = w;
        BetterChatRenderer.suggestH = h;
        BetterChatRenderer.suggestItemH = itemH;
        BetterChatRenderer.suggestRowsX = rowX;
        BetterChatRenderer.suggestRowsY = rowY;
        BetterChatRenderer.suggestRowsW = rowW;
        BetterChatRenderer.suggestRowsH = rowH;
        BetterChatRenderer.suggestStart = BetterChatRenderer.suggestCurrentStart;
        BetterChatRenderer.suggestVisible = visible;
        BetterChatRenderer.suggestTotal = total;
        BetterChatRenderer.suggestWindow = CommandSuggestorBridge.lastWindow();
        BetterChatRenderer.suggestHasScrollbar = hasScrollbar;
        BetterChatRenderer.suggestTrackX = trackX;
        BetterChatRenderer.suggestTrackY = trackY;
        BetterChatRenderer.suggestTrackW = trackW;
        BetterChatRenderer.suggestTrackH = trackH;
        BetterChatRenderer.suggestThumbH = thumbH;
        if (!hasScrollbar) BetterChatRenderer.suggestDraggingScrollbar = false;
        BetterChatRenderer.storedScreen = null;
    }
    private static ScreenRect computeBox(CommandSuggestorBridge.SuggestionSnapshot snap, float baseFont, int visible, float itemH) {
        float screenW = Minecraft.getInstance().getWindow().getWidth();
        float h = itemH * visible + 6f;
        float x;
        float w;
        float y;
        if (BetterChatRenderer.hasInputBox && BetterChatRenderer.inputBoxW > 0f) {
            x = BetterChatRenderer.inputBoxX;
            float needed = textWidthRange(snap.texts(), snap.startIndex(), visible, baseFont) + 18f;
            w = Math.max(140f, Math.min(BetterChatRenderer.inputBoxW, Math.max(needed, 0f)));
            y = BetterChatRenderer.inputBoxY - h - 4f;
        } else {
            double scale = Minecraft.getInstance().getWindow().getGuiScale();
            x = Math.max(4f, (float) (snap.x() * scale));
            float snapW = snap.w() > 0 ? (float) (snap.w() * scale) : 200f;
            float needed = textWidthRange(snap.texts(), snap.startIndex(), visible, baseFont) + 18f;
            w = Math.max(140f, Math.max(needed, snapW));
            y = (float) (snap.y() * scale) - h - 2f;
        }
        if (y < 4f) y = 4f;
        if (x + w > screenW - 4f) x = screenW - w - 4f;
        return new ScreenRect(x, y, w, h);
    }
    private static float textWidthRange(List<String> list, int start, int count, float fontSize) {
        float max = 0f;
        for (int i = 0; i < count; i++) {
            int idx = start + i;
            if (idx < 0 || idx >= list.size()) break;
            max = Math.max(max, BetterChatFontUtil.textWidth(BetterChatFontUtil.getIosevkaRegular(), list.get(idx), fontSize));
        }
        return max;
    }
    private record ScreenRect(float x, float y, float w, float h) {
    }
}
