package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config;

import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

import java.util.List;

final class ConfigProfilesListRenderer {
    private final ConfigProfilesState state;
    private final ConfigProfileCardComponent cardComponent;

    ConfigProfilesListRenderer(ConfigProfilesState state, ConfigProfileCardComponent cardComponent) {
        this.state = state;
        this.cardComponent = cardComponent;
    }

    void renderCards(float x, float y, float w, float h, List<ConfigProfileMeta> entries, float mx, float my, float scale, SettingsGuiPalette palette) {
        float gap = 6f * scale;
        int columns = w > 230f * scale ? 2 : 1;
        float cardW = (w - gap * (columns - 1)) / columns;
        float cardH = 58f * scale;
        float stepY = cardH + gap;
        int rows = (entries.size() + columns - 1) / columns;
        float contentH = rows <= 0 ? 0f : rows * stepY - gap;
        float maxScroll = Math.max(0f, contentH - h);
        updateScrollbarMetrics(x, y, w, h, maxScroll, scale);
        if (state.draggingScrollbar()) scrollToMouse(ClickGuiRenderer.getMouseY());

        state.scroll(ClickGuiMath.clamp(state.scroll(), -maxScroll, 0f));
        state.smoothedScroll(AnimationUtility.approach(state.smoothedScroll(), state.scroll(), state.draggingScrollbar() ? 0.55f : 0.2f));
        state.smoothedScroll(AnimationUtility.snap(state.smoothedScroll(), state.scroll(), state.draggingScrollbar() ? 0.01f : 0.05f));

        if (entries.isEmpty()) {
            String empty = ClickGuiSearch.isActive() && !ClickGuiSearch.getText().isBlank()
                    ? ClickGuiI18n.tr("clickgui.settings.config.empty.no_matches", "No matching configs.")
                    : ClickGuiI18n.tr("clickgui.settings.config.empty.none_saved", "No saved configs.");
            float emptySize = 8.0f * scale;
            float emptyW = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), empty, emptySize);
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), empty, x + (w - emptyW) * 0.5f, y + h * 0.42f, emptySize, palette.panelMuted(), false);
            if (!ClickGuiSearch.isActive() || ClickGuiSearch.getText().isBlank()) {
                String hint = ClickGuiI18n.tr("clickgui.settings.config.empty.drop_hint", "Drop .lvcfg files here, or put them anywhere under config/lovisual/profiles/.");
                float hintSize = 6.9f * scale;
                String fitted = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterRegular(), hint, hintSize, w - 18f * scale);
                float hintW = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), fitted, hintSize);
                ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), fitted, x + (w - hintW) * 0.5f, y + h * 0.42f + 13f * scale, hintSize, palette.panelMuted(), false);
            }
            return;
        }
        boolean clipped = ScissorFunction.pushRaw(x, y, w, h);
        for (int i = 0; i < entries.size(); i++) {
            int col = i % columns;
            int row = i / columns;
            float cx = x + col * (cardW + gap);
            float cy = y + row * stepY + state.smoothedScroll();
            if (cy + cardH < y || cy > y + h) continue;
            ConfigProfileMeta meta = entries.get(i);
            boolean selectedState = state.selected() != null && state.selected().getId().equals(meta.getId()) && state.selected().type() == meta.type();
            var hit = cardComponent.render(meta, cx, cy, cardW, cardH, mx, my, selectedState, scale, palette);
            if (state.isEditing(meta)) {
                state.editingTitleRect(new ConfigProfilesState.Rect(hit.title().x(), hit.title().y(), hit.title().w(), hit.title().h()));
                renderRenameInput(state.editingTitleRect(), scale, palette);
            }
            if (hit.title() != null && ClickGuiMath.insideRect(mx, my, hit.title().x(), hit.title().y(), hit.title().w(), hit.title().h())) {
                SystemCursor.set(SystemCursor.CursorType.MOVE);
            }
            state.hits().add(new ConfigProfilesState.CardEntryHit(meta, hit));
        }
        if (clipped) ScissorFunction.pop();
        renderScrollbar(x, y, w, h, maxScroll, scale, palette);
    }

    void renderStatus(float x, float y, float w, float scale, SettingsGuiPalette palette) {
        if (!state.isStatusVisible()) return;
        float size = 6.8f * scale;
        String text = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterRegular(), state.statusMessage(), size, w - 176f * scale);
        float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterRegular(), text, size);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), text, x + w - tw, y + 7.2f * scale, size, palette.panelMuted(), false);
    }

    private void renderRenameInput(ConfigProfilesState.Rect rect, float scale, SettingsGuiPalette palette) {
        float padX = 2f * scale;
        float yy = rect.y() - scale;
        float hh = rect.h() + 2f * scale;
        LayoutRender2D.roundedQuad(rect.x() - padX, yy, rect.w() + padX * 2f, hh, 3f * scale,
                SettingsGuiPalette.mix(palette.moduleCardTop(), palette.menuCategorySelectedLeft(), 0.28f),
                SettingsGuiPalette.mix(palette.moduleCardTopStrong(), palette.menuCategorySelectedRight(), 0.22f),
                SettingsGuiPalette.mix(palette.moduleCardTopStrong(), palette.menuCategorySelectedRight(), 0.22f),
                SettingsGuiPalette.mix(palette.moduleCardTop(), palette.menuCategorySelectedLeft(), 0.28f));
        String text = ClickGuiRenderer.fitText(ClickGuiRenderer.getInterMedium(), state.editingText(), 8.8f * scale, rect.w() - 5f * scale);
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterMedium(), text, rect.x(), rect.y(), 8.8f * scale, palette.moduleTitleText(), false);
        if ((System.currentTimeMillis() / 500L) % 2L == 0L) {
            float tw = ClickGuiRenderer.textWidth(ClickGuiRenderer.getInterMedium(), text, 8.8f * scale);
            LayoutRender2D.rect(rect.x() + Math.min(tw + 1.5f * scale, rect.w() - 2f * scale), rect.y() + 1.2f * scale, 0.6f * scale, 8f * scale, palette.moduleTitleText());
        }
    }

    void renderScrollbar(float x, float y, float w, float h, float maxScroll, float scale, SettingsGuiPalette palette) {
        updateScrollbarMetrics(x, y, w, h, maxScroll, scale);
        if (!state.scrollbarVisible()) return;
        float mx = ClickGuiRenderer.getMouseX();
        float my = ClickGuiRenderer.getMouseY();
        if (isScrollbarHovered(mx, my) || state.draggingScrollbar()) SystemCursor.set(SystemCursor.CursorType.SCROLL);
        LayoutRender2D.roundedQuad(state.scrollbarX(), state.scrollbarY(), state.scrollbarW(), state.scrollbarH(), 2f * scale,
                palette.moduleScrollTrackA(), palette.moduleScrollTrackB(), palette.moduleScrollTrackB(), palette.moduleScrollTrackA());
        LayoutRender2D.roundedQuad(state.scrollbarX(), state.scrollbarThumbY(), state.scrollbarW(), state.scrollbarThumbH(), 2f * scale,
                palette.moduleScrollHandleA(), palette.moduleScrollHandleB(), palette.moduleScrollHandleB(), palette.moduleScrollHandleA());
    }

    void updateScrollbarMetrics(float x, float y, float w, float h, float maxScroll, float scale) {
        boolean visible = maxScroll > 0.5f;
        state.scrollbarVisible(visible);
        state.scrollbarMaxScroll(Math.max(0f, maxScroll));
        if (!visible) {
            state.draggingScrollbar(false);
            state.scrollbarX(0f); state.scrollbarY(0f); state.scrollbarW(0f); state.scrollbarH(0f); state.scrollbarThumbY(0f); state.scrollbarThumbH(0f);
            return;
        }
        state.scrollbarW(3f * scale);
        state.scrollbarX(x + w - state.scrollbarW() - 2f * scale);
        state.scrollbarY(y + 3f * scale);
        state.scrollbarH(Math.max(1f, h - 6f * scale));
        state.scrollbarThumbH(Math.max(18f * scale, state.scrollbarH() * (state.scrollbarH() / (state.scrollbarMaxScroll() + state.scrollbarH()))));
        float ratio = state.scrollbarMaxScroll() <= 0f ? 0f : (-state.smoothedScroll() / state.scrollbarMaxScroll());
        state.scrollbarThumbY(state.scrollbarY() + (state.scrollbarH() - state.scrollbarThumbH()) * AnimationUtility.clamp(ratio, 0f, 1f));
    }

    boolean isScrollbarHovered(float mx, float my) {
        if (!state.scrollbarVisible()) return false;
        float pad = 4f;
        return ClickGuiMath.insideRect(mx, my, state.scrollbarX() - pad, state.scrollbarY() - pad, state.scrollbarW() + pad * 2f, state.scrollbarH() + pad * 2f);
    }

    void scrollToMouse(float my) {
        if (!state.scrollbarVisible() || state.scrollbarMaxScroll() <= 0f) return;
        float span = state.scrollbarH() - state.scrollbarThumbH();
        if (span <= 0.5f) return;
        float thumbTop = AnimationUtility.clamp(my - state.scrollbarDragOffset(), state.scrollbarY(), state.scrollbarY() + span);
        float ratio = (thumbTop - state.scrollbarY()) / span;
        state.scroll((float) (-state.scrollbarMaxScroll() * AnimationUtility.clamp(ratio, 0f, 1f)));
    }
}
