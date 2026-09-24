package dev.loki.lovisual.features.gui.clickgui.settings.skin;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;
import dev.loki.lovisual.features.gui.clickgui.settings.color.ColorSettingMath;
import dev.loki.lovisual.features.gui.clickgui.settings.skin.UnifiedSettingsSkin;

public final class SettingSkinUtil {
    private SettingSkinUtil() {
    }

    public static float m(float settings, float modules) {
        return UnifiedSettingsSkin.metric(settings, modules);
    }

    public static float s() {
        return UnifiedSettingsSkin.scale();
    }

    public static boolean modules() {
        return UnifiedSettingsSkin.modules();
    }

    public static void renderOptionContainer(float x, float y, float w, float h, boolean active) {
        float alpha = active ? 0.22f : 0.12f;
        if (active) {
            UnifiedSettingsSkin.drawAccent(x, y, Math.max(1f, w), Math.max(m(14f, 10f), h), m(3f, 3f), alpha);
        } else {
            UnifiedSettingsSkin.drawSurface(x, y, Math.max(1f, w), Math.max(m(14f, 10f), h), m(3f, 3f), 0.34f);
        }
    }

    public static void renderChipButton(float x, float y, float w, float h, String label, float hoverAnim, boolean active) {
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        int bg = active ? UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.ACCENT, 70) : UnifiedSettingsSkin.SURFACE_SOFT;
        bg = UnifiedSettingsSkin.mix(bg, UnifiedSettingsSkin.SURFACE_HOVER, hoverAnim * 0.72f);
        ClickGuiRenderer.drawRoundedRect(x, y, w, h, m(4f, 4f), bg);
        int stroke = active
                ? UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.ACCENT, 135 + Math.round(hoverAnim * 55f))
                : UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, 58 + Math.round(hoverAnim * 65f));
        ClickGuiRenderer.drawRoundedRectStroke(x, y, w, h, m(4f, 4f), m(0.7f, 0.45f), stroke);
        float size = Math.min(m(13f, 6f), Math.max(m(10f, 4.8f), (w - m(4f, 4f)) / Math.max(1f, label.length()) * 1.45f));
        float tw = UnifiedSettingsSkin.textWidth(font, label, size);
        float ty = y + (h - UnifiedSettingsSkin.textHeight(font, size)) * 0.5f - m(0.2f, 0.2f);
        int color = active ? UnifiedSettingsSkin.ACCENT : UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, UnifiedSettingsSkin.TEXT_PRIMARY, 0.68f + hoverAnim * 0.32f);
        ClickGuiRenderer.drawText(font, label, x + Math.max(m(2f, 2f), (w - tw) * 0.5f), ty, size, color, false);
    }

    public static void renderRainbowChipButton(float x, float y, float w, float h, float hoverAnim, boolean active) {
        float t = (System.nanoTime() % 3_000_000_000L) / 3_000_000_000f;
        int c0 = ColorSettingMath.hsbToArgb((t + 0.00f) % 1f, 0.80f, 1.0f, active ? 205 : 92 + Math.round(hoverAnim * 34f));
        int c1 = ColorSettingMath.hsbToArgb((t + 0.18f) % 1f, 0.80f, 1.0f, active ? 215 : 96 + Math.round(hoverAnim * 38f));
        int c2 = ColorSettingMath.hsbToArgb((t + 0.36f) % 1f, 0.80f, 1.0f, active ? 215 : 96 + Math.round(hoverAnim * 38f));
        int c3 = ColorSettingMath.hsbToArgb((t + 0.54f) % 1f, 0.80f, 1.0f, active ? 205 : 92 + Math.round(hoverAnim * 34f));
        if (active) {
            ClickGuiRenderer.drawRoundedRectGradient(x, y, w, h, m(4f, 4f), c0, c2, 25f + t * 360f);
            ClickGuiRenderer.drawRoundedRect(x, y, w, h, m(4f, 4f), UnifiedSettingsSkin.withAlpha(0xFF000000, 18 - Math.round(hoverAnim * 6f)));
            ClickGuiRenderer.drawRoundedRectStrokeGradient(x, y, w, h, m(4f, 4f), m(0.8f, 0.45f), c0, c2, 25f + t * 360f);
        } else {
            int bg = UnifiedSettingsSkin.mix(UnifiedSettingsSkin.SURFACE_SOFT, UnifiedSettingsSkin.SURFACE_HOVER, hoverAnim * 0.75f);
            ClickGuiRenderer.drawRoundedRect(x, y, w, h, m(4f, 4f), UnifiedSettingsSkin.withAlpha(bg, 70 + Math.round(hoverAnim * 42f)));
            if (hoverAnim > 0.02f) {
                ClickGuiRenderer.drawRoundedRectGradient(x, y, w, h, m(4f, 4f), UnifiedSettingsSkin.withAlpha(c0, Math.round(52f * hoverAnim)), UnifiedSettingsSkin.withAlpha(c2, Math.round(52f * hoverAnim)), 25f + t * 360f);
                ClickGuiRenderer.drawRoundedRectStrokeGradient(x, y, w, h, m(4f, 4f), m(0.7f, 0.45f), UnifiedSettingsSkin.withAlpha(c0, 80 + Math.round(96f * hoverAnim)), UnifiedSettingsSkin.withAlpha(c2, 80 + Math.round(96f * hoverAnim)), 25f + t * 360f);
            } else {
                ClickGuiRenderer.drawRoundedRectStroke(x, y, w, h, m(4f, 4f), m(0.7f, 0.45f), UnifiedSettingsSkin.withAlpha(UnifiedSettingsSkin.TEXT_FAINT, 72));
            }
        }
        TextRenderer font = UnifiedSettingsSkin.fontMedium();
        String label = "RNB";
        float size = Math.min(m(13f, 6f), Math.max(m(10f, 4.8f), (w - m(4f, 4f)) / Math.max(1f, label.length()) * 1.45f));
        float tw = UnifiedSettingsSkin.textWidth(font, label, size);
        float ty = y + (h - UnifiedSettingsSkin.textHeight(font, size)) * 0.5f - m(0.2f, 0.2f);
        int labelColor = active ? 0xFFFFFFFF : UnifiedSettingsSkin.mix(UnifiedSettingsSkin.TEXT_MUTED, 0xFFFFFFFF, hoverAnim * 0.62f);
        ClickGuiRenderer.drawText(font, label, x + Math.max(m(2f, 2f), (w - tw) * 0.5f), ty, size, labelColor, false);
    }

    public static void renderChecker(float x, float y, float w, float h, float cell, float radius) {
        int light = 0xFFE7E7E7;
        int dark = 0xFF777777;
        ClickGuiRenderer.drawRoundedRect(x, y, w, h, radius, light);
        float safeCell = Math.max(1f, cell);
        int cols = Math.max(1, (int) Math.ceil(w / safeCell));
        int rows = Math.max(1, (int) Math.ceil(h / safeCell));
        boolean clipped = ScissorFunction.pushRaw(x, y, w, h);
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (((row + col) & 1) == 0) continue;
                ClickGuiRenderer.drawRect(x + col * safeCell, y + row * safeCell, safeCell, safeCell, dark);
            }
        }
        if (clipped) ScissorFunction.pop();
    }

    public static void drawBindMarquee(
            TextRenderer font,
            String text,
            float x,
            float y,
            float fullWidth,
            float viewWidth,
            float fontSize,
            int color
    ) {
        final float speed = 22.0f;
        final float gap = 14.0f;
        final float pauseSec = 0.8f;

        if (text == null || text.isEmpty()) return;
        if (speed <= 0.0f) {
            ClickGuiRenderer.drawText(font, text, x, y, fontSize, color, false);
            return;
        }

        float cycleDistance = fullWidth + gap;
        float cycleTime = pauseSec + cycleDistance / speed;
        if (cycleTime <= 0.0f) {
            ClickGuiRenderer.drawText(font, text, x, y, fontSize, color, false);
            return;
        }

        float now = net.minecraft.util.Util.getMillis() / 1000.0f;
        float t = now % cycleTime;

        float offset = 0.0f;
        if (t > pauseSec) {
            offset = -(t - pauseSec) * speed;
        }

        ClickGuiRenderer.drawText(font, text, x + offset, y, fontSize, color, false);

        if (cycleDistance > viewWidth * 0.5f) {
            ClickGuiRenderer.drawText(font, text, x + offset + cycleDistance, y, fontSize, color, false);
        }
    }
}
