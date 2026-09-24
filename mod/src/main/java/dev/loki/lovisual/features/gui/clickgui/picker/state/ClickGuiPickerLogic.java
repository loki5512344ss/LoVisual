package dev.loki.lovisual.features.gui.clickgui.picker;

import dev.loki.lovisual.features.gui.clickgui.picker.catalog.PickerEntryData;
import java.util.*;
import net.minecraft.world.item.ItemStack;

final class ClickGuiPickerLogic {
    static final Comparator<PickerEntryData> ENTRY_ORDER = Comparator
            .comparing(PickerEntryData::label, String.CASE_INSENSITIVE_ORDER)
            .thenComparing(PickerEntryData::id, String.CASE_INSENSITIVE_ORDER);

    private ClickGuiPickerLogic() {}

    static void reloadEntries(ClickGuiPickerState s) {
        s.allEntries.clear();
        s.allEntries.addAll(s.catalog.entries(s.owner));
    }

    static void refreshFiltered(ClickGuiPickerState s, boolean resetScroll) {
        String needle = s.search.toString().trim().toLowerCase(Locale.ROOT);
        s.byId.clear();
        for (PickerEntryData e : s.allEntries) s.byId.put(e.getId(), e);
        s.visibleAll.clear();
        for (PickerEntryData e : s.allEntries) if (matches(e, needle)) s.visibleAll.add(e);
        s.visibleAll.sort(ENTRY_ORDER);
        s.visibleSelected.clear();
        for (String id : s.owner.getValueSet()) {
            PickerEntryData e = s.byId.get(id);
            if (e == null) e = new PickerEntryData(id, id, ItemStack.EMPTY);
            if (!matches(e, needle)) continue;
            s.visibleSelected.add(e);
        }
        s.visibleSelected.sort(ENTRY_ORDER);
        if (resetScroll) {
            s.scroll = 0f;
            s.smoothScroll = 0f;
            s.draggingScrollbar = false;
        } else clampScroll(s);
    }

    static List<PickerEntryData> visibleEntries(ClickGuiPickerState s) {
        return s.showAll ? s.visibleAll : s.visibleSelected;
    }

    static void toggleSelection(ClickGuiPickerState s, String id) {
        Set<String> next = s.owner.getValueSet();
        if (next.contains(id)) next.remove(id);
        else next.add(s.owner.normalizeEntry(id));
        s.owner.setValueSet(next);
        if (!next.contains(s.focusedId)) {
            s.focusedId = next.isEmpty() ? null : next.iterator().next();
            var d = s.detailOwner();
            if (d != null && s.focusedId != null) d.onPickerFocusChanged(s.focusedId);
        }
        refreshFiltered(s, false);
    }

    static boolean matches(PickerEntryData e, String needle) {
        if (needle == null || needle.isBlank()) return true;
        String id = e.getId() == null ? "" : e.getId().toLowerCase(Locale.ROOT);
        String label = e.label() == null ? "" : e.label().toLowerCase(Locale.ROOT);
        return id.contains(needle) || label.contains(needle);
    }

    static void layout(ClickGuiPickerState s, int fbw, int fbh) {
        s.windowX = (fbw - s.windowW()) * 0.5f;
        s.windowY = (fbh - ClickGuiPickerState.WINDOW_H) * 0.5f;
        s.closeX = s.windowX + s.windowW() - ClickGuiPickerState.PADDING - ClickGuiPickerState.CLOSE_SIZE;
        s.closeY = s.windowY + 7f * ClickGuiPickerState.SCALE;
        s.searchX = s.closeX - 3f * ClickGuiPickerState.SCALE - ClickGuiPickerState.SEARCH_W;
        s.searchY = s.windowY + 6f * ClickGuiPickerState.SCALE;
        s.filterX = s.searchX - 3f * ClickGuiPickerState.SCALE - ClickGuiPickerState.FILTER_W;
        s.filterY = s.searchY;
        s.gridX = s.windowX + ClickGuiPickerState.PADDING;
        s.gridY = s.windowY + ClickGuiPickerState.HEADER_H + 6f * ClickGuiPickerState.SCALE;
        var d = s.detailOwner();
        s.detailW = d != null ? d.pickerDetailWidth() : 0f;
        s.detailY = s.gridY;
        s.detailH = ClickGuiPickerState.WINDOW_H - (s.gridY - s.windowY) - ClickGuiPickerState.PADDING;
        s.detailX = s.windowX + s.windowW() - ClickGuiPickerState.PADDING - s.detailW;
        s.gridW = s.windowW() - ClickGuiPickerState.PADDING * 2f - (d != null ? s.detailW + ClickGuiPickerState.CARD_GAP : 0f);
        s.gridH = s.detailH;
    }

    static int columns(ClickGuiPickerState s) {
        return Math.max(1, (int) Math.floor((s.gridW + ClickGuiPickerState.CARD_GAP) / (ClickGuiPickerState.CARD_SIZE + ClickGuiPickerState.CARD_GAP)));
    }

    static float computeContentHeight(ClickGuiPickerState s, int count) {
        if (count <= 0) return 0f;
        int cols = columns(s);
        int rows = (count + cols - 1) / cols;
        return rows * ClickGuiPickerState.CARD_SIZE + Math.max(0, rows - 1) * ClickGuiPickerState.CARD_GAP;
    }

    static void clampScroll(ClickGuiPickerState s) {
        s.contentHeight = computeContentHeight(s, visibleEntries(s).size());
        float min = Math.min(0f, s.gridH - s.contentHeight);
        s.scroll = clamp(s.scroll, min, 0f);
        s.smoothScroll = clamp(s.smoothScroll, min, 0f);
        s.scrollbarVisible = s.contentHeight > s.gridH + 0.5f;
    }

    static boolean handleScrollbarMouseDown(ClickGuiPickerState s, float mx, float my) {
        if (!s.scrollbarVisible) return false;
        if (!isScrollbarHovered(s, mx, my)) return false;
        s.draggingScrollbar = true;
        scrollToPosition(s, my);
        return true;
    }

    static boolean isScrollbarHovered(ClickGuiPickerState s, float mx, float my) {
        if (!s.scrollbarVisible) return false;
        float pad = 3f * ClickGuiPickerState.SCALE;
        return mx >= s.sbTrackX - pad && mx <= s.sbTrackX + s.sbTrackW + pad && my >= s.sbTrackY - pad && my <= s.sbTrackY + s.sbTrackH + pad;
    }

    static void scrollToPosition(ClickGuiPickerState s, float my) {
        if (s.contentHeight <= s.gridH + 0.5f) { s.scroll = 0f; s.smoothScroll = 0f; return; }
        float span = s.sbTrackH - s.sbThumbH;
        if (span <= 0f) return;
        float rel = my - s.sbTrackY - s.sbThumbH * 0.5f;
        float t = clamp(rel / span, 0f, 1f);
        float range = s.contentHeight - s.gridH;
        s.scroll = -range * t;
        s.smoothScroll = s.scroll;
    }

    static float clamp(float v, float min, float max) {
        if (v < min) return min;
        return Math.min(v, max);
    }
}
