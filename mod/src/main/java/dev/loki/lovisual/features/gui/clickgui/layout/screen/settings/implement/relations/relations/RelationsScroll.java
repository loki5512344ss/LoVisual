
package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

final class RelationsScroll {
    void renderScrollbar(RelationsState state, float x, float y, float w, float h, float maxScroll, float scale, SettingsGuiPalette palette) {
        updateMetrics(state, x, y, w, h, maxScroll, scale);
        if (!state.scrollbarVisible) return;
        float mx = ClickGuiRenderer.getMouseX();
        float my = ClickGuiRenderer.getMouseY();
        if (isHovered(state, mx, my) || state.draggingScrollbar) SystemCursor.set(SystemCursor.CursorType.SCROLL);
        LayoutRender2D.roundedQuad(state.scrollbarX, state.scrollbarY, state.scrollbarW, state.scrollbarH, 2f * scale, palette.moduleScrollTrackA(), palette.moduleScrollTrackB(), palette.moduleScrollTrackB(), palette.moduleScrollTrackA());
        LayoutRender2D.roundedQuad(state.scrollbarX, state.scrollbarThumbY, state.scrollbarW, state.scrollbarThumbH, 2f * scale, palette.moduleScrollHandleA(), palette.moduleScrollHandleB(), palette.moduleScrollHandleB(), palette.moduleScrollHandleA());
    }

    void updateMetrics(RelationsState state, float x, float y, float w, float h, float maxScroll, float scale) {
        state.scrollbarVisible = maxScroll > 0.5f;
        state.scrollbarMaxScroll = Math.max(0f, maxScroll);
        if (!state.scrollbarVisible) {
            state.draggingScrollbar = false;
            state.scrollbarX = state.scrollbarY = state.scrollbarW = state.scrollbarH = state.scrollbarThumbY = state.scrollbarThumbH = 0f;
            return;
        }
        state.scrollbarW = 3f * scale;
        state.scrollbarX = x + w - state.scrollbarW - 2f * scale;
        state.scrollbarY = y + 3f * scale;
        state.scrollbarH = Math.max(1f, h - 6f * scale);
        state.scrollbarThumbH = Math.max(18f * scale, state.scrollbarH * (state.scrollbarH / (state.scrollbarMaxScroll + state.scrollbarH)));
        float ratio = state.scrollbarMaxScroll <= 0f ? 0f : (-state.smoothedScroll / state.scrollbarMaxScroll);
        state.scrollbarThumbY = state.scrollbarY + (state.scrollbarH - state.scrollbarThumbH) * AnimationUtility.clamp(ratio, 0f, 1f);
    }

    boolean isHovered(RelationsState state, float mx, float my) {
        if (!state.scrollbarVisible) return false;
        float pad = 4f;
        return ClickGuiMath.insideRect(mx, my, state.scrollbarX - pad, state.scrollbarY - pad, state.scrollbarW + pad * 2f, state.scrollbarH + pad * 2f);
    }

    void scrollToMouse(RelationsState state, float my) {
        if (!state.scrollbarVisible || state.scrollbarMaxScroll <= 0f) return;
        float span = state.scrollbarH - state.scrollbarThumbH;
        if (span <= 0.5f) return;
        float thumbTop = AnimationUtility.clamp(my - state.scrollbarDragOffset, state.scrollbarY, state.scrollbarY + span);
        float ratio = (thumbTop - state.scrollbarY) / span;
        state.scroll = -state.scrollbarMaxScroll * AnimationUtility.clamp(ratio, 0f, 1f);
    }

    boolean press(RelationsState state, float mx, float my, int button) {
        if (button != 0 || !isHovered(state, mx, my)) return false;
        state.draggingScrollbar = true;
        state.scrollbarDragOffset = ClickGuiMath.insideRect(mx, my, state.scrollbarX, state.scrollbarThumbY, state.scrollbarW, state.scrollbarThumbH) ? my - state.scrollbarThumbY : state.scrollbarThumbH * 0.5f;
        scrollToMouse(state, my);
        return true;
    }
}
