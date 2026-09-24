package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.main;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

/**
 * Scroll and scrollbar state for {@link MainSettingsComponent}'s settings panel: owns the
 * scroll offset, its smoothing animation and the scrollbar geometry/hit-testing. The
 * component drives it once per frame via {@link #layout} and forwards input events here.
 */
final class MainSettingsScroll {
    private float scroll;
    private float smoothed;
    private boolean dragging;
    private boolean visible;
    private float sbX;
    private float sbY;
    private float sbW;
    private float sbH;
    private float thumbY;
    private float thumbH;
    private float maxScroll;
    private float dragOffset;

    void reset() {
        scroll = 0f;
        smoothed = 0f;
        dragging = false;
    }

    void addWheel(double amount) {
        scroll += (float) (amount * 22f);
    }

    float smoothed() {
        return smoothed;
    }

    boolean needsScrollbar() {
        return visible;
    }

    boolean press(float mx, float my, int button) {
        if (button != 0 || !isHovered(mx, my)) return false;
        dragging = true;
        dragOffset = ClickGuiMath.insideRect(mx, my, sbX, thumbY, sbW, thumbH) ? my - thumbY : thumbH * 0.5f;
        scrollToMouse(my);
        return true;
    }

    void release(int button) {
        if (button == 0) dragging = false;
    }

    boolean isHovered(float mx, float my) {
        if (!visible) return false;
        float pad = 4f;
        return ClickGuiMath.insideRect(mx, my, sbX - pad, sbY - pad, sbW + pad * 2f, sbH + pad * 2f);
    }

    void layout(float rightX, float rightW, float contentY, float contentH, float totalH, float my, float scale) {
        float max = Math.max(0f, totalH - contentH);
        updateMetrics(max, totalH, contentY, contentH, rightX, rightW, scale);
        if (dragging) scrollToMouse(my);
        scroll = ClickGuiMath.clamp(scroll, -max, 0f);
        smoothed = AnimationUtility.approach(smoothed, scroll, dragging ? 0.55f : 0.2f);
        smoothed = AnimationUtility.snap(smoothed, scroll, dragging ? 0.01f : 0.05f);
    }

    private void updateMetrics(float max, float totalH, float contentY, float contentH, float rightX, float rightW, float scale) {
        visible = max > 0f && totalH > contentH + 0.5f;
        maxScroll = max;
        if (!visible) {
            dragging = false;
            sbX = sbY = sbW = sbH = thumbY = thumbH = 0f;
            return;
        }
        sbW = 4f * scale;
        sbX = rightX + rightW - 10f * scale;
        sbY = contentY;
        sbH = contentH;
        thumbH = Math.max(18f * scale, sbH * (contentH / Math.max(contentH, totalH)));
        float ratio = maxScroll <= 0f ? 0f : (-smoothed / maxScroll);
        thumbY = sbY + (sbH - thumbH) * AnimationUtility.clamp(ratio, 0f, 1f);
    }

    private void scrollToMouse(float my) {
        if (!visible || maxScroll <= 0f) return;
        float span = sbH - thumbH;
        if (span <= 0.5f) return;
        float thumbTop = AnimationUtility.clamp(my - dragOffset, sbY, sbY + span);
        float ratio = (thumbTop - sbY) / span;
        scroll = -maxScroll * AnimationUtility.clamp(ratio, 0f, 1f);
    }

    void render(float scale, SettingsGuiPalette palette) {
        if (!visible) return;
        float mx = ClickGuiRenderer.getMouseX();
        float my = ClickGuiRenderer.getMouseY();
        if (isHovered(mx, my) || dragging) {
            SystemCursor.set(SystemCursor.CursorType.SCROLL);
        }
        LayoutRender2D.roundedQuad(sbX, sbY, sbW, sbH, 2f * scale,
                palette.moduleScrollTrackA(), palette.moduleScrollTrackB(), palette.moduleScrollTrackB(), palette.moduleScrollTrackA());
        LayoutRender2D.roundedQuad(sbX, thumbY, sbW, thumbH, 2f * scale,
                palette.moduleScrollHandleA(), palette.moduleScrollHandleB(), palette.moduleScrollHandleB(), palette.moduleScrollHandleA());
    }
}
