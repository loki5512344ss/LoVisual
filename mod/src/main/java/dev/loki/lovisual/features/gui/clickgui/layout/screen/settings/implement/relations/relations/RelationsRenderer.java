
package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import java.util.List;

final class RelationsRenderer {
    final RelationsScroll scroll = new RelationsScroll();

    void render(RelationsState state, float menuX, float menuY, float menuW, float menuH, float mx, float my, float scale) {
        state.cardHits.clear();
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        state.updateMovementBlocked();
        float areaX = menuX + 31f * scale;
        float areaY = menuY + 33f * scale;
        float areaW = menuW - 42f * scale;
        float areaH = menuH - 39f * scale;
        renderToolbar(state, areaX, areaY, areaW, mx, my, scale, palette);
        float y = areaY + 24f * scale;
        renderStatus(state, areaX, y, areaW, scale, palette);
        y += 13f * scale;
        state.listX = areaX;
        state.listY = y;
        state.listW = areaW;
        state.listH = Math.max(1f, areaY + areaH - y);
        if (state.onlinePicker.isVisible()) {
            state.onlinePicker.render(state.listX, state.listY, state.listW, state.listH, mx, my, scale, palette);
            return;
        }
        renderCards(state, state.listX, state.listY, state.listW, state.listH, state.filteredEntries(state.tab.entries()), mx, my, scale, palette);
    }

    void renderToolbar(RelationsState state, float x, float y, float w, float mx, float my, float scale, SettingsGuiPalette palette) {
        float pillW = 165f * scale;
        float pillH = 15f * scale;
        float targetMode = state.tab.ordinal();
        float dt = AnimationUtility.deltaTime();
        state.modeAnim = AnimationUtility.approach(state.modeAnim, targetMode, dt, 12f);
        state.modeAnim = AnimationUtility.snap(state.modeAnim, targetMode, 0.01f);
        state.friendsPill = new RelationsState.Rect(x, y, pillW / 3f, pillH);
        state.enemiesPill = new RelationsState.Rect(x + pillW / 3f, y, pillW / 3f, pillH);
        state.staffPill = new RelationsState.Rect(x + pillW * 2f / 3f, y, pillW / 3f, pillH);
        state.friendsHoverAnim = updateHover(state.friendsHoverAnim, state.friendsPill.contains(mx, my), dt);
        state.enemiesHoverAnim = updateHover(state.enemiesHoverAnim, state.enemiesPill.contains(mx, my), dt);
        state.staffHoverAnim = updateHover(state.staffHoverAnim, state.staffPill.contains(mx, my), dt);
        ClickGuiRenderer.drawBlur(x, y, pillW, pillH, 4f * scale, palette.panelBlurTint(), 145f / 255f);
        LayoutRender2D.roundedQuad(x, y, pillW, pillH, 3.5f * scale, SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgLeft(), 0.34f), SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgRight(), 0.44f), SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgRight(), 0.44f), SettingsGuiPalette.mix(palette.panelPillBase(), palette.panelBgLeft(), 0.34f));
        LayoutRender2D.roundedStrokeQuad(x, y, pillW, pillH, 3.5f * scale, 0.5f * scale, palette.panelStroke(), palette.moduleDividerEnd(), palette.moduleDividerEnd(), palette.panelStroke());
        float pad = 1.2f * scale;
        float segment = (pillW - pad * 2f) / 3f;
        float activeX = x + pad + segment * state.modeAnim;
        LayoutRender2D.roundedQuad(activeX, y + pad, segment, pillH - pad * 2f, 3f * scale, SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedLeft(), 0.22f), SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedRight(), 0.22f), SettingsGuiPalette.mix(SettingsGuiPalette.darken(palette.panelPillActive(), 0.16f), palette.menuCategorySelectedRight(), 0.18f), SettingsGuiPalette.mix(palette.panelPillActive(), palette.menuCategorySelectedLeft(), 0.22f));
        drawToolbarSeparator(x + pad + segment, y, pillH, scale, palette);
        drawToolbarSeparator(x + pad + segment * 2f, y, pillH, scale, palette);
        drawSegment(state.friendsPill, state.tr("tab.friends", "Friends"), state.tab == RelationsState.RelationTab.FRIENDS, state.friendsHoverAnim, scale, palette);
        drawSegment(state.enemiesPill, state.tr("tab.enemies", "Enemies"), state.tab == RelationsState.RelationTab.ENEMIES, state.enemiesHoverAnim, scale, palette);
        drawSegment(state.staffPill, state.tr("tab.staff", "Staff"), state.tab == RelationsState.RelationTab.STAFF, state.staffHoverAnim, scale, palette);
        float fieldX = x + pillW + 8f * scale;
        float picker = 18f * scale;
        float action = 18f * scale;
        float gap = 4f * scale;
        boolean onlineMode = state.onlinePicker.isOpen();
        float fieldW = Math.max(80f * scale, w - (fieldX - x) - picker - action - gap * 2f);
        state.playerInputRect = new RelationsState.Rect(fieldX, y, fieldW, pillH);
        state.addTypedButton = new RelationsState.Rect(fieldX + fieldW + gap, y, action, pillH);
        state.reservedPickerButton = new RelationsState.Rect(state.addTypedButton.x() + action + gap, y, picker, pillH);
        if (onlineMode) {
            String query = state.onlinePicker.searchText();
            drawInput(state.playerInputRect, query, state.tr("placeholder.search_online", "Search online"), state.onlinePicker.isSearchFocused(), mx, my, scale, palette);
            state.addHoverAnim = updateHover(state.addHoverAnim, state.addTypedButton.contains(mx, my), dt);
            drawIconButton(state.addTypedButton, "x", state.addHoverAnim, query == null || query.isBlank(), scale, palette);
        } else {
            drawInput(state.playerInputRect, state.playerInput, state.tr("placeholder.nick", "Nick"), state.activeField == RelationsState.ActiveField.PLAYER, mx, my, scale, palette);
            state.addHoverAnim = updateHover(state.addHoverAnim, state.addTypedButton.contains(mx, my), dt);
            drawIconButton(state.addTypedButton, "check", state.addHoverAnim, false, scale, palette);
        }
        state.pickerHoverAnim = updateHover(state.pickerHoverAnim, state.reservedPickerButton.contains(mx, my) || state.onlinePicker.isVisible(), dt);
        drawIconButton(state.reservedPickerButton, onlineMode ? "x" : "user-plus", state.pickerHoverAnim, false, scale, palette);
    }

    void renderCards(RelationsState state, float x, float y, float w, float h, List<String> entries, float mx, float my, float scale, SettingsGuiPalette palette) {
        float gap = 6f * scale;
        float reservedScrollbarW = 9f * scale;
        float cardAreaW = Math.max(1f, w - reservedScrollbarW);
        int columns = cardAreaW > 230f * scale ? 2 : 1;
        float cardW = (cardAreaW - gap * (columns - 1)) / columns;
        float cardH = 36f * scale;
        float stepY = cardH + gap;
        int rows = (entries.size() + columns - 1) / columns;
        float contentH = rows <= 0 ? 0f : rows * stepY - gap;
        float maxScroll = Math.max(0f, contentH - h);
        scroll.updateMetrics(state, x, y, w, h, maxScroll, scale);
        if (state.draggingScrollbar) scroll.scrollToMouse(state, ClickGuiRenderer.getMouseY());
        state.scroll = ClickGuiMath.clamp(state.scroll, -maxScroll, 0f);
        state.smoothedScroll = AnimationUtility.approach(state.smoothedScroll, state.scroll, state.draggingScrollbar ? 0.55f : 0.2f);
        state.smoothedScroll = AnimationUtility.snap(state.smoothedScroll, state.scroll, state.draggingScrollbar ? 0.01f : 0.05f);
        if (entries.isEmpty()) {
            String empty = ClickGuiSearch.isActive() && !ClickGuiSearch.getText().isBlank() ? state.tr("empty.no_matches", "No matching players.") : state.tr("empty.none_added", "No players added.");
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), empty, x + 4f * scale, y + 4f * scale, 7.4f * scale, palette.panelMuted(), false);
            return;
        }
        boolean clipped = ScissorFunction.pushRaw(x, y, w, h);
        for (int i = 0; i < entries.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            float cx = x + col * (cardW + gap);
            float cy = y + row * stepY + state.smoothedScroll;
            if (cy + cardH < y || cy > y + h) continue;
            String name = entries.get(i);
            RelationPlayerCardComponent.CardHit hit = state.cardComponent.render(name, cx, cy, cardW, cardH, mx, my, scale, palette);
            state.cardHits.add(new RelationsState.CardEntryHit(name, hit));
        }
        if (clipped) ScissorFunction.pop();
        scroll.renderScrollbar(state, x, y, w, h, maxScroll, scale, palette);
    }

    void drawSegment(RelationsState.Rect rect, String label, boolean active, float hover, float scale, SettingsGuiPalette palette) {
        float size = 7.5f * scale;
        float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), label, size);
        float th = ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterMedium(), size);
        int color = active ? palette.panelText() : SettingsGuiPalette.mix(palette.panelMuted(), palette.panelText(), 0.20f * hover);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), label, rect.x() + (rect.w() - tw) * 0.5f, rect.y() + (rect.h() - th) * 0.5f, size, color, false);
    }

    void drawToolbarSeparator(float x, float y, float h, float scale, SettingsGuiPalette palette) {
        float backingW = Math.max(1.25f * scale, 1.05f);
        float backingH = h - 4.4f * scale;
        float backingY = y + (h - backingH) * 0.5f;
        int backingTop = SettingsGuiPalette.withAlpha(palette.panelBgLeft(), 98);
        int backingBottom = SettingsGuiPalette.withAlpha(palette.panelBgRight(), 78);
        LayoutRender2D.roundedQuad(x - backingW * 0.5f, backingY, backingW, backingH, backingW * 0.5f, backingTop, backingTop, backingBottom, backingBottom);
        float lineW = Math.max(0.5f * scale, 0.75f);
        float lineH = h - 6.0f * scale;
        float lineY = y + (h - lineH) * 0.5f;
        int top = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.moduleDividerStart(), palette.menuCategorySelectedLeft(), 0.34f), 146);
        int mid = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.moduleDividerEnd(), palette.menuCategorySelectedRight(), 0.40f), 210);
        int bottom = SettingsGuiPalette.withAlpha(SettingsGuiPalette.mix(palette.moduleDividerEnd(), palette.panelText(), 0.24f), 136);
        LayoutRender2D.roundedQuad(x - lineW * 0.5f, lineY, lineW, lineH, lineW * 0.5f, top, mid, bottom, top);
    }

    void drawInput(RelationsState.Rect rect, String value, String placeholder, boolean active, float mx, float my, float scale, SettingsGuiPalette palette) {
        boolean hover = rect.contains(mx, my);
        int bgA = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverLeft(), hover || active ? 0.20f : 0.06f);
        int bgB = SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverRight(), hover || active ? 0.18f : 0.06f);
        LayoutRender2D.roundedQuad(rect.x(), rect.y(), rect.w(), rect.h(), 3.5f * scale, bgA, bgB, bgB, bgA);
        LayoutRender2D.roundedStrokeQuad(rect.x(), rect.y(), rect.w(), rect.h(), 3.5f * scale, 0.45f * scale, active ? palette.moduleDividerEnd() : LayoutRender2D.alpha(palette.moduleDividerStart(), 0.75f), active ? palette.moduleDividerEnd() : LayoutRender2D.alpha(palette.moduleDividerEnd(), 0.75f), active ? palette.moduleDividerEnd() : LayoutRender2D.alpha(palette.moduleDividerEnd(), 0.65f), active ? palette.moduleDividerStart() : LayoutRender2D.alpha(palette.moduleDividerStart(), 0.75f));
        boolean empty = value == null || value.isEmpty();
        String raw = empty ? (active ? "" : placeholder) : value;
        int color = empty && !active ? palette.panelMuted() : palette.panelText();
        float size = 6.8f * scale;
        String text = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterRegular(), raw, size, rect.w() - 8f * scale);
        float textY = rect.y() + (rect.h() - ClickGuiRenderer.textHeight(ClickGuiRenderer.getInterRegular(), size)) * 0.5f;
        if (!text.isEmpty()) ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), text, rect.x() + 4f * scale, textY, size, color, false);
        if (active && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), text, size);
            LayoutRender2D.rect(rect.x() + Math.min(tw + 5.5f * scale, rect.w() - 3f * scale), rect.y() + 3f * scale, 0.6f * scale, rect.h() - 6f * scale, palette.panelText());
        }
    }

    void drawIconButton(RelationsState.Rect rect, String icon, float hover, boolean disabled, float scale, SettingsGuiPalette palette) {
        int bgA = disabled ? LayoutRender2D.alpha(palette.panelPillBase(), 0.42f) : SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverLeft(), 0.12f + hover * 0.22f);
        int bgB = disabled ? LayoutRender2D.alpha(palette.panelPillBase(), 0.32f) : SettingsGuiPalette.mix(palette.panelPillBase(), palette.menuCategoryHoverRight(), 0.12f + hover * 0.24f);
        LayoutRender2D.roundedQuad(rect.x(), rect.y(), rect.w(), rect.h(), 3.5f * scale, bgA, bgB, bgB, bgA);
        int iconColor = disabled ? LayoutRender2D.alpha(palette.panelMuted(), 0.58f) : palette.menuCategoryText();
        float iconSize = Math.min(rect.w(), rect.h()) - 7f * scale;
        Renderer2D.COLOR.svg(icon, rect.x() + (rect.w() - iconSize) * 0.5f, rect.y() + (rect.h() - iconSize) * 0.5f, iconSize, iconSize, SvgRenderOptions.overrideColor(iconColor));
    }

    void renderStatus(RelationsState state, float x, float y, float w, float scale, SettingsGuiPalette palette) {
        if (state.statusMessage == null || state.statusMessage.isBlank()) return;
        if (System.currentTimeMillis() > state.statusUntilMs) {
            state.clearStatus();
            return;
        }
        float size = 6.8f * scale;
        String text = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterRegular(), state.statusMessage, size, w - 8f * scale);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), text, x + 4f * scale, y + 2.6f * scale, size, palette.panelMuted(), false);
    }

    float updateHover(float current, boolean hovered, float dt) {
        return AnimationUtility.snap(AnimationUtility.approach(current, hovered ? 1f : 0f, dt, 12f), hovered ? 1f : 0f, 0.01f);
    }
}
