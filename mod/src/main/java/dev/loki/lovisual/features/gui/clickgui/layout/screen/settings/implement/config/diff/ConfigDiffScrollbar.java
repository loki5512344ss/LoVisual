package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config.diff;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.helpers.util.SystemCursor;

/**
 * Scroll state of the diff panel: wheel accumulation, clamping/smoothing of the
 * offset, thumb geometry, drag handling and painting. The panel drives it in
 * phases — {@link #begin} (geometry), {@link #dragToMouse} while dragging,
 * {@link #smooth} (offset), then {@link #draw}.
 */
public final class ConfigDiffScrollbar {
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
    private float scroll;
    private float smoothed;

    public void reset() {
        scroll = 0f;
        smoothed = 0f;
        dragging = false;
    }

    public void release() {
        dragging = false;
    }

    public boolean visible() {
        return visible;
    }

    public boolean dragging() {
        return dragging;
    }

    /** Smoothed offset the body is painted at. */
    public float offset() {
        return smoothed;
    }

    /** Wheel input: true when a scrollbar exists (a bare panel absorbs the event). */
    public boolean wheel(double amount) {
        if (!visible && maxScroll <= 0.5f) return false;
        scroll += (float) (amount * 20f);
        return true;
    }

    public boolean press(float mx, float my) {
        if (!hovered(mx, my)) return false;
        dragging = true;
        dragOffset = ClickGuiMath.insideRect(mx, my, x, thumbY, w, thumbH)
                ? my - thumbY
                : thumbH * 0.5f;
        dragToMouse(my);
        return true;
    }

    /** Thumb geometry for the current body rect and content height. */
    public void begin(float x, float y, float w, float h, float maxScroll, float scale) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        visible = maxScroll > 0.5f;
        this.maxScroll = Math.max(0f, maxScroll);
        if (!visible) {
            dragging = false;
            this.x = this.y = this.w = this.h = thumbY = thumbH = 0f;
            return;
        }
        this.w = 3f * scale;
        this.x = x + w - this.w - 1.5f * scale;
        this.y = y + 2f * scale;
        this.h = Math.max(1f, h - 4f * scale);
        thumbH = Math.max(16f * scale, this.h * (this.h / (this.maxScroll + this.h)));
        float ratio = this.maxScroll <= 0f ? 0f : (-smoothed / this.maxScroll);
        thumbY = this.y + (this.h - thumbH) * AnimationUtility.clamp(ratio, 0f, 1f);
    }

    /** Clamp + smooth the offset toward the target for this frame. */
    public void smooth() {
        scroll = ClickGuiMath.clamp(scroll, -maxScroll, 0f);
        smoothed = AnimationUtility.approach(smoothed, scroll, dragging ? 0.55f : 0.2f);
        smoothed = AnimationUtility.snap(smoothed, scroll, dragging ? 0.01f : 0.05f);
    }

    public void dragToMouse(float my) {
        if (!dragging) return;
        if (!visible || maxScroll <= 0f) return;
        float span = h - thumbH;
        if (span <= 0.5f) return;
        float thumbTop = AnimationUtility.clamp(my - dragOffset, y, y + span);
        float ratio = (thumbTop - y) / span;
        scroll = -maxScroll * AnimationUtility.clamp(ratio, 0f, 1f);
    }

    public void draw(float scale, SettingsGuiPalette palette, float openAnim) {
        if (!visible) return;
        float mx = ClickGuiRenderer.getMouseX();
        float my = ClickGuiRenderer.getMouseY();
        if (hovered(mx, my) || dragging) SystemCursor.set(SystemCursor.CursorType.SCROLL);

        LayoutRender2D.roundedQuad(x, y, w, h, 2f * scale,
                LayoutRender2D.alpha(palette.moduleScrollTrackA(), openAnim),
                LayoutRender2D.alpha(palette.moduleScrollTrackB(), openAnim),
                LayoutRender2D.alpha(palette.moduleScrollTrackB(), openAnim),
                LayoutRender2D.alpha(palette.moduleScrollTrackA(), openAnim));
        LayoutRender2D.roundedQuad(x, thumbY, w, thumbH, 2f * scale,
                LayoutRender2D.alpha(palette.moduleScrollHandleA(), openAnim),
                LayoutRender2D.alpha(palette.moduleScrollHandleB(), openAnim),
                LayoutRender2D.alpha(palette.moduleScrollHandleB(), openAnim),
                LayoutRender2D.alpha(palette.moduleScrollHandleA(), openAnim));
    }

    /** Body width lost to the scrollbar column, when there is one. */
    public float gutter(float scale) {
        return visible ? 7f * scale : 0f;
    }

    private boolean hovered(float mx, float my) {
        if (!visible) return false;
        float pad = 4f;
        return ClickGuiMath.insideRect(mx, my, x - pad, y - pad, w + pad * 2f, h + pad * 2f);
    }
}
