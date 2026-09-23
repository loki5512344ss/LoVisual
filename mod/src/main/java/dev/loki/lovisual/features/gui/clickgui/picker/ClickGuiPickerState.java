/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.picker;

import dev.loki.lovisual.features.gui.clickgui.picker.catalog.PickerCatalog;
import dev.loki.lovisual.features.gui.clickgui.picker.catalog.PickerCatalogFactory;
import dev.loki.lovisual.features.gui.clickgui.picker.catalog.PickerEntryData;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.settings.owner.PickerDetailOwner;
import dev.loki.lovisual.features.gui.clickgui.settings.text.list.TextListSetting;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import java.util.*;

public final class ClickGuiPickerState {
    static final float SCALE = 2f;
    static final float WINDOW_W = 340f * SCALE;
    static final float WINDOW_H = 260f * SCALE;
    static final float PADDING = 8f * SCALE;
    static final float HEADER_H = 28f * SCALE;
    static final float SEARCH_H = 15f * SCALE;
    static final float FILTER_H = SEARCH_H;
    static final float SEARCH_W = 98f * SCALE;
    static final float FILTER_W = 80f * SCALE;
    static final float CLOSE_SIZE = 12f * SCALE;
    static final float CARD_SIZE = 38f * SCALE;
    static final float CARD_GAP = 5f * SCALE;
    static final float SCROLL_STEP = 15f * SCALE;
    static final float MARQUEE_SPEED = 18f * SCALE;
    static final float MARQUEE_GAP = 10f * SCALE;
    static final float MARQUEE_PAUSE_SEC = 0.55f;

    final TextListSetting owner;
    final TextListSetting.PickerMode mode;
    final PickerCatalog catalog;
    final String title;
    final StringBuilder search = new StringBuilder();
    final List<PickerEntryData> allEntries = new ArrayList<>();
    final List<PickerEntryData> visibleAll = new ArrayList<>();
    final List<PickerEntryData> visibleSelected = new ArrayList<>();
    final Map<String, PickerEntryData> byId = new LinkedHashMap<>();
    final List<CardHit> cardHits = new ArrayList<>();
    boolean showAll = true;
    boolean listening;
    float modeAnim;
    float scroll;
    float smoothScroll;
    boolean draggingScrollbar;
    boolean scrollbarVisible;
    float sbTrackX, sbTrackY, sbTrackW, sbTrackH, sbThumbH, contentHeight;
    float windowX, windowY, searchX, searchY, filterX, filterY, closeX, closeY;
    float gridX, gridY, gridW, gridH, detailX, detailY, detailW, detailH;
    String focusedId;
    TextRenderer interBold;
    String hoveredCardId;
    long hoveredCardStartMs;
    float allFilterHoverAnim, selectedFilterHoverAnim;

    public ClickGuiPickerState(TextListSetting owner, String title, TextListSetting.PickerMode mode) {
        this.owner = owner;
        this.mode = mode == null ? TextListSetting.PickerMode.TEXT : mode;
        this.catalog = PickerCatalogFactory.forMode(this.mode);
        this.title = title == null || title.isBlank() ? "Select" : title;
        this.reloadEntries();
        this.refreshFiltered(true);
    }

    static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public boolean isListening() { return listening; }
    public void toggleListening() { listening = !listening; }
    public void stopListening() { listening = false; }

    public void insertChar(char c) {
        if (Character.isISOControl(c)) return;
        search.append(c); refreshFiltered(true);
    }
    public void backspace() {
        if (search.length() <= 0) return;
        search.deleteCharAt(search.length() - 1); refreshFiltered(true);
    }

    public void handleMouseMove(float mx, float my) {
        if (!draggingScrollbar) return;
        ClickGuiPickerLogic.layout(this, ClickGuiRenderer.framebufferWidth(), ClickGuiRenderer.framebufferHeight());
        ClickGuiPickerLogic.scrollToPosition(this, my);
    }

    public void handleMouseDown(float mx, float my, int button) {
        if (button != 0) return;
        ClickGuiPickerLogic.layout(this, ClickGuiRenderer.framebufferWidth(), ClickGuiRenderer.framebufferHeight());
        if (!inside(mx, my, windowX, windowY, windowW(), WINDOW_H)) { ClickGuiRenderer.closePickerScreen(); return; }
        if (inside(mx, my, closeX, closeY, CLOSE_SIZE, CLOSE_SIZE)) { ClickGuiRenderer.closePickerScreen(); return; }
        if (inside(mx, my, searchX, searchY, SEARCH_W, SEARCH_H)) { listening = true; return; }
        listening = false;
        if (inside(mx, my, filterX, filterY, FILTER_W, FILTER_H)) {
            boolean nextShowAll = mx < filterX + FILTER_W * 0.5f;
            if (showAll != nextShowAll) { showAll = nextShowAll; scroll = 0f; smoothScroll = 0f; draggingScrollbar = false; ClickGuiPickerLogic.clampScroll(this); }
            return;
        }
        PickerDetailOwner d = detailOwner();
        if (d != null && inside(mx, my, detailX, detailY, detailW, detailH) && d.mouseClickedPickerDetails(mx, my, button)) return;
        if (ClickGuiPickerLogic.handleScrollbarMouseDown(this, mx, my)) return;
        for (CardHit hit : cardHits) {
            if (!inside(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) continue;
            focusedId = hit.getId();
            if (d != null) { d.onPickerFocusChanged(hit.getId()); if (!d.shouldToggleSelectionOnCardClick()) return; }
            ClickGuiPickerLogic.toggleSelection(this, hit.getId()); return;
        }
    }

    public void handleMouseUp(float mx, float my, int button) {
        PickerDetailOwner d = detailOwner();
        if (d != null) d.mouseReleasedPickerDetails(mx, my, button);
        if (button == 0) draggingScrollbar = false;
    }

    public void scroll(double delta) {
        ClickGuiPickerLogic.layout(this, ClickGuiRenderer.framebufferWidth(), ClickGuiRenderer.framebufferHeight());
        PickerDetailOwner d = detailOwner();
        if (d != null) {
            float mx = ClickGuiRenderer.getMouseX(); float my = ClickGuiRenderer.getMouseY();
            if (inside(mx, my, detailX, detailY, detailW, detailH) && d.mouseScrolledPickerDetails(mx, my, delta)) return;
        }
        scroll += (float) (delta * SCROLL_STEP); ClickGuiPickerLogic.clampScroll(this);
    }

    public void render(int fbw, int fbh) {
        ClickGuiPickerLogic.layout(this, fbw, fbh); ClickGuiPickerLogic.clampScroll(this);
        float targetMode = showAll ? 0f : 1f;
        modeAnim = AnimationUtility.approach(modeAnim, targetMode, 0.22f); modeAnim = AnimationUtility.snap(modeAnim, targetMode, 0.01f);
        smoothScroll = AnimationUtility.approach(smoothScroll, scroll, 0.2f); smoothScroll = AnimationUtility.snap(smoothScroll, scroll, 0.05f);
        float mx = ClickGuiRenderer.getMouseX(); float my = ClickGuiRenderer.getMouseY();
        SettingsGuiPalette p = SettingsGuiPalette.current(); Themes.Theme t = Theme.theme();
        ClickGuiPickerChromeRenderer.drawWindow(this, p);
        ClickGuiPickerChromeRenderer.drawHeader(this, p);
        ClickGuiPickerChromeRenderer.drawCloseButton(this, mx, my, p, t);
        ClickGuiPickerFilterRenderer.drawFilterPill(this, mx, my, p, t);
        ClickGuiPickerChromeRenderer.drawSearchField(this, mx, my, p, t);
        ClickGuiPickerGridRenderer.drawGrid(this, mx, my, p, t);
        drawPickerDetails(mx, my);
        ClickGuiPickerGridRenderer.drawScrollbar(this, mx, my, p);
    }

    float windowW() { PickerDetailOwner d = detailOwner(); return WINDOW_W + (d != null ? d.pickerDetailWidth() : 0f); }
    PickerDetailOwner detailOwner() { return owner instanceof PickerDetailOwner d ? d : null; }
    void drawPickerDetails(float mx, float my) {
        PickerDetailOwner d = detailOwner(); if (d == null) return;
        d.renderPickerDetails(detailX, detailY, detailW, detailH, focusedId, mx, my);
    }

    private void reloadEntries() { ClickGuiPickerLogic.reloadEntries(this); }
    private void refreshFiltered(boolean reset) { ClickGuiPickerLogic.refreshFiltered(this, reset); }

    record CardHit(String id, float x, float y, float w, float h) { public String getId() { return id; } }
}
