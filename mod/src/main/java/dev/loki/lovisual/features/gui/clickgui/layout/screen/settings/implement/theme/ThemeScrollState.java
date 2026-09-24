package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.theme;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

/**
 * Scroll + scrollbar state for {@link ThemeComponent}: raw and smoothed offsets,
 * thumb geometry, drag handling and the on-screen track/thumb painting. Kept
 * separate so the component facade stays focused on card layout and hit routing.
 */
final class ThemeScrollState {

    private float scroll;
    private float smoothedScroll;
    private boolean visible;
    private boolean dragging;
    private float x;
    private float y;
    private float w;
    private float h;
    private float thumbY;
    private float thumbH;
    private float maxScroll;
    private float dragOffset;

    /** Reset both raw and smoothed scroll plus the drag flag. */
    void reset() {
        scroll = 0f;
        smoothedScroll = 0f;
        dragging = false;
    }

    /** Add a wheel delta expressed in pixels. */
    void addDelta(float amount) {
        scroll += amount;
    }

    /** Begin a thumb drag if the cursor is over the scrollbar. Returns true if consumed. */
    boolean press(float mx, float my, int button) {
        if (button != 0) return false;
        if (!hovered(mx, my)) return false;
        dragging = true;
        dragOffset = ClickGuiMath.insideRect(mx, my, x, thumbY, w, thumbH) ? my - thumbY : thumbH * 0.5f;
        scrollToMouse(my);
        return true;
    }

    /** End a thumb drag on primary-button release. */
    void release(int button) {
        if (button == 0) dragging = false;
    }

    /** Recompute thumb geometry from the current layout; call after {@link #smooth()}. */
    void updateMetrics(float menuX, float menuY, float menuH, float areaH, float contentH, float maxScroll, float scale) {
        visible = maxScroll > 0f && contentH > areaH + 0.5f;
        this.maxScroll = Math.max(0f, maxScroll);
        if (!visible) {
            dragging = false;
            x = y = w = h = thumbY = thumbH = 0f;
            return;
        }
        w = 4f * scale;
        x = menuX + 400f * scale - 35f * scale - w + 50f * scale;
        y = menuY + 33f * scale;
        h = menuH - 58f * scale;
        thumbH = Math.max(20f * scale, h * (areaH / contentH));
        float ratio = this.maxScroll <= 0f ? 0f : (-smoothedScroll / this.maxScroll);
        thumbY = y + (h - thumbH) * AnimationUtility.clamp(ratio, 0f, 1f);
    }

    /** Clamp the raw offset and animate the smoothed value towards it. */
    void smooth() {
        scroll = ClickGuiMath.clamp(scroll, -maxScroll, 0f);
        smoothedScroll = AnimationUtility.approach(smoothedScroll, scroll, dragging ? 0.55f : 0.2f);
        smoothedScroll = AnimationUtility.snap(smoothedScroll, scroll, dragging ? 0.01f : 0.05f);
    }

    /** Whether the pointer is close enough to the scrollbar strip to interact with it. */
    boolean hovered(float mx, float my) {
        if (!visible) return false;
        float pad = 4f;
        return ClickGuiMath.insideRect(mx, my, x - pad, y - pad, w + pad * 2f, h + pad * 2f);
    }

    /** Jump the raw scroll so the thumb sits under {@code my}. */
    void scrollToMouse(float my) {
        if (!visible || maxScroll <= 0f) return;
        float span = h - thumbH;
        if (span <= 0.5f) return;
        float thumbTop = AnimationUtility.clamp(my - dragOffset, y, y + span);
        scroll = -maxScroll * AnimationUtility.clamp((thumbTop - y) / span, 0f, 1f);
    }

    /** Paint the track + thumb (and toggle the scroll cursor while hovering). */
    void render(float scale, SettingsGuiPalette palette) {
        if (!visible) return;
        if (hovered(ClickGuiRenderer.getMouseX(), ClickGuiRenderer.getMouseY()) || dragging) {
            SystemCursor.set(SystemCursor.CursorType.SCROLL);
        }
        LayoutRender2D.roundedQuad(x, y, w, h, 2f * scale,
                palette.moduleScrollTrackA(), palette.moduleScrollTrackB(),
                palette.moduleScrollTrackB(), palette.moduleScrollTrackA());
        LayoutRender2D.roundedQuad(x, thumbY, w, thumbH, 2f * scale,
                palette.moduleScrollHandleA(), palette.moduleScrollHandleB(),
                palette.moduleScrollHandleB(), palette.moduleScrollHandleA());
    }

    float smoothed() {
        return smoothedScroll;
    }

    boolean dragging() {
        return dragging;
    }

    boolean isVisible() {
        return visible;
    }
}
