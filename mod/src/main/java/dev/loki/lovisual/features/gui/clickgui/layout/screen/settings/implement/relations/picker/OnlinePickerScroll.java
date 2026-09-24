
package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

final class OnlinePickerScroll {
    float scroll;
    float smoothedScroll;
    boolean draggingScrollbar;
    boolean scrollbarVisible;
    float scrollbarX;
    float scrollbarY;
    float scrollbarW;
    float scrollbarH;
    float scrollbarThumbY;
    float scrollbarThumbH;
    float scrollbarMaxScroll;
    float scrollbarDragOffset;

    void reset() { scroll = 0f; smoothedScroll = 0f; draggingScrollbar = false; }
    void clampAndSmooth(float maxScroll) {
        scroll = ClickGuiMath.clamp(scroll, -maxScroll, 0f);
        smoothedScroll = AnimationUtility.approach(smoothedScroll, scroll, draggingScrollbar ? 0.55f : 0.22f);
        smoothedScroll = AnimationUtility.snap(smoothedScroll, scroll, draggingScrollbar ? 0.01f : 0.05f);
    }
    void addScroll(float amount) { scroll += amount; }
    void updateMetrics(float x, float y, float w, float h, float maxScroll, float scale) {
        scrollbarVisible = maxScroll > 0.5f;
        scrollbarMaxScroll = Math.max(0f, maxScroll);
        if (!scrollbarVisible) { draggingScrollbar = false; scrollbarX = scrollbarY = scrollbarW = scrollbarH = scrollbarThumbY = scrollbarThumbH = 0f; return; }
        scrollbarW = 3f * scale;
        scrollbarX = x + w - scrollbarW - 2f * scale;
        scrollbarY = y + 3f * scale;
        scrollbarH = Math.max(1f, h - 6f * scale);
        scrollbarThumbH = Math.max(18f * scale, scrollbarH * (scrollbarH / (scrollbarMaxScroll + scrollbarH)));
        float ratio = scrollbarMaxScroll <= 0f ? 0f : (-smoothedScroll / scrollbarMaxScroll);
        scrollbarThumbY = scrollbarY + (scrollbarH - scrollbarThumbH) * AnimationUtility.clamp(ratio, 0f, 1f);
    }
    boolean isHovered(float mx, float my) {
        if (!scrollbarVisible) return false;
        float pad = 4f;
        return ClickGuiMath.insideRect(mx, my, scrollbarX - pad, scrollbarY - pad, scrollbarW + pad * 2f, scrollbarH + pad * 2f);
    }
    void scrollToMouse(float my) {
        if (!scrollbarVisible || scrollbarMaxScroll <= 0f) return;
        float span = scrollbarH - scrollbarThumbH;
        if (span <= 0.5f) return;
        float top = AnimationUtility.clamp(my - scrollbarDragOffset, scrollbarY, scrollbarY + span);
        float ratio = (top - scrollbarY) / span;
        scroll = -scrollbarMaxScroll * AnimationUtility.clamp(ratio, 0f, 1f);
    }
    boolean press(float mx, float my, int button) {
        if (button != 0 || !isHovered(mx, my)) return false;
        draggingScrollbar = true;
        scrollbarDragOffset = ClickGuiMath.insideRect(mx, my, scrollbarX, scrollbarThumbY, scrollbarW, scrollbarThumbH) ? my - scrollbarThumbY : scrollbarThumbH * 0.5f;
        scrollToMouse(my);
        return true;
    }
    void release(int button) { if (button == 0) draggingScrollbar = false; }
    void dragIfNeeded() { if (draggingScrollbar) scrollToMouse(ClickGuiRenderer.getMouseY()); }
    void renderScrollbar(float x, float y, float w, float h, float maxScroll, float scale, SettingsGuiPalette palette, float opacity) {
        updateMetrics(x, y, w, h, maxScroll, scale);
        if (!scrollbarVisible) return;
        if (isHovered(ClickGuiRenderer.getMouseX(), ClickGuiRenderer.getMouseY()) || draggingScrollbar) SystemCursor.set(SystemCursor.CursorType.SCROLL);
        LayoutRender2D.roundedQuad(scrollbarX, scrollbarY, scrollbarW, scrollbarH, 2f * scale, fade(palette.panelScrollTrackA(), opacity), fade(palette.panelScrollTrackB(), opacity), fade(palette.panelScrollTrackB(), opacity), fade(palette.panelScrollTrackA(), opacity));
        LayoutRender2D.roundedQuad(scrollbarX, scrollbarThumbY, scrollbarW, scrollbarThumbH, 2f * scale, fade(palette.panelScrollHandleA(), opacity), fade(palette.panelScrollHandleB(), opacity), fade(palette.panelScrollHandleB(), opacity), fade(palette.panelScrollHandleA(), opacity));
    }
    static int fade(int c, float o) { return LayoutRender2D.alpha(c, AnimationUtility.clamp01(o)); }
}
