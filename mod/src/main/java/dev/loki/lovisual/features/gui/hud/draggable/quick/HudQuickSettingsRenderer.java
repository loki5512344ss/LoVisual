package dev.loki.lovisual.features.gui.hud.draggable.quick;

import dev.loki.lovisual.config.values.collection.ConfigValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.theme.Theme;
import dev.loki.lovisual.features.theme.Themes;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;

/** Draws {@link HudQuickSettings} in the HUD pipeline. */
public final class HudQuickSettingsRenderer {
    private static final float TEXT_SCALE = 0.8f;

    private HudQuickSettingsRenderer() {
    }

    public static void render(Renderer2D renderer, TextRenderer text) {
        if (!HudQuickSettings.isOpen() || text == null) return;
        Themes.Theme theme = Theme.theme();
        float x = HudQuickSettings.x();
        float y = HudQuickSettings.y();
        float w = HudQuickSettings.WIDTH;
        float h = HudQuickSettings.height();
        renderer.roundedRect(x, y, w, h, 5f, 1f, HudRenderUtil.setAlpha(theme.windowBg(), 0xEE));
        renderer.roundedRectStroke(x, y, w, h, 5f, 1f, 0.8f, HudRenderUtil.setAlpha(theme.windowStroke(), 0xC0));

        text.begin(TEXT_SCALE, false, false);
        try {
            float th = (float) text.getHeight(false);
            text.render(HudQuickSettings.widget().getTitle(), x + HudQuickSettings.PAD, y + (HudQuickSettings.HEADER_H - th) / 2f,
                    new RenderColor(theme.accent()), false);
            float rowY = y + HudQuickSettings.HEADER_H;
            for (Setting row : HudQuickSettings.rows()) {
                float rh = HudQuickSettings.rowH(row);
                drawRow(renderer, text, theme, row, x, rowY, rh, th);
                rowY += rh;
            }
        } finally {
            text.end();
        }
    }

    private static void drawRow(Renderer2D renderer, TextRenderer text, Themes.Theme theme, Setting row,
                                float x, float rowY, float rh, float th) {
        float pad = HudQuickSettings.PAD;
        float right = x + HudQuickSettings.WIDTH - pad;
        ConfigValue<?> value = row.getConfigValue();
        float labelY = value instanceof NumberValue<?> ? rowY + 2f : rowY + (rh - th) / 2f;
        text.render(row.getDisplayName(), x + pad, labelY, new RenderColor(theme.textPrimary()), false);
        if (value instanceof BooleanValue flag) {
            float sw = 16f;
            float sh = 8f;
            float sy = rowY + (rh - sh) / 2f;
            renderer.roundedRect(right - sw, sy, sw, sh, sh / 2f, 1f, flag.get() ? theme.accent() : theme.strokeSoft());
            float knob = sh - 2f;
            renderer.roundedRect(flag.get() ? right - knob - 1f : right - sw + 1f, sy + 1f, knob, knob, knob / 2f, 1f, theme.textPrimary());
        } else if (value instanceof NumberValue<?> number) {
            String shown = number.toDisplay();
            text.render(shown, right - (float) text.getWidth(shown, false), labelY, new RenderColor(theme.textMuted()), false);
            float ty = rowY + rh - 6f;
            float tw = HudQuickSettings.trackW();
            float ratio = (float) QuickSliderMath.ratio(number.get().doubleValue(), number.getMin().doubleValue(), number.getMax().doubleValue());
            renderer.roundedRect(HudQuickSettings.trackX(), ty, tw, 3f, 1.5f, 1f, theme.strokeSoft());
            renderer.roundedRect(HudQuickSettings.trackX(), ty, Math.max(3f, tw * ratio), 3f, 1.5f, 1f, theme.accent());
        } else {
            String shown = value instanceof ModeValue mode ? row.getOptionDisplayName(mode.get())
                    : value instanceof EnumValue<?> option ? row.getOptionDisplayName(option.getId()) : "";
            text.render(shown, right - (float) text.getWidth(shown, false), labelY, new RenderColor(theme.accentSoft()), false);
        }
    }
}
