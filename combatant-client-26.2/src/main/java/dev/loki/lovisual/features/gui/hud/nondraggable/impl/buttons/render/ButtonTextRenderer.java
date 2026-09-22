/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */
package dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.render;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.nondraggable.impl.buttons.BetterButtons;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.text.format.LegacyTextUtil;
import dev.loki.lovisual.util.text.format.TextRenderUtil;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.List;
import static dev.loki.lovisual.features.theme.Theme.theme;
/**
 * Текстовый слой кнопок: центрированный/левый выравниваемый, legacy-цвета, обрезка с "...".
 */
public final class ButtonTextRenderer {
    private static final float TEXT_SCALE_BASE = 0.42f;
    private static final float TEXT_PADDING_X = 6.0f;
    private static final float TEXT_Y_OFFSET = 0.0f;
    private ButtonTextRenderer() {
    }
    public static void renderCentered(Component text, float x, float y, float w, float h, float alpha, boolean enabled, float hover) {
        renderCentered(text, x, y, w, h, alpha, enabled, hover, true);
    }
    public static void renderCentered(Component text, float x, float y, float w, float h, float alpha, boolean enabled, float hover, boolean snapToPixel) {
        TextRenderer tr = Fonts.renderer("Monsterrat", FontInfo.Type.Regular, TextRenderer.get());
        if (tr == null) return;
        if (text == null || text.getString().isEmpty()) return;
        float padX = resolvePaddingX();
        float scale = resolveScaleBase() * (h / 20.0f);
        int baseText = resolveTextColor(enabled, hover);
        List<TextRenderUtil.FlatPart> parts = flattenLegacy(text, baseText);
        parts = trimParts(tr, parts, w - padX * 2f, scale, baseText);
        if (parts.isEmpty()) return;
        double tw = measurePartsWidth(tr, parts, scale);
        double th = measureHeight(tr, scale);
        float tx = (float) (x + (w - tw) * 0.5f);
        float ty = (float) (y + (h - th) * 0.5f + TEXT_Y_OFFSET);
        if (snapToPixel) {
            tx = Math.round(tx);
            ty = Math.round(ty);
        }
        tr.begin(scale, false, false);
        float cx = tx;
        for (TextRenderUtil.FlatPart part : parts) {
            if (part.text().isEmpty()) continue;
            int color = HudRenderUtil.scaleAlpha(part.color(), alpha);
            tr.render(part.text(), cx, ty, new RenderColor(color), false);
            cx += (float) tr.getWidth(part.text(), false);
        }
        tr.end();
    }
    public static void renderLeftAligned(Component text, float x0, float y0, float x1, float y1, float alpha, boolean enabled, float hover) {
        renderLeftAligned(text, x0, y0, x1, y1, alpha, enabled, hover, true);
    }
    public static void renderLeftAligned(Component text, float x0, float y0, float x1, float y1, float alpha, boolean enabled, float hover, boolean snapToPixel) {
        TextRenderer tr = Fonts.renderer("Monsterrat", FontInfo.Type.Regular, TextRenderer.get());
        if (tr == null) return;
        if (text == null || text.getString().isEmpty()) return;
        float w = x1 - x0;
        float h = y1 - y0;
        float padX = resolvePaddingX();
        float scale = resolveScaleBase() * (h / 20.0f);
        int baseText = resolveTextColor(enabled, hover);
        List<TextRenderUtil.FlatPart> parts = flattenLegacy(text, baseText);
        parts = trimParts(tr, parts, w - padX, scale, baseText);
        if (parts.isEmpty()) return;

        double th = measureHeight(tr, scale);
        float tx = x0 + padX * 0.5f;
        float ty = (float) (y0 + (h - th) * 0.5f + TEXT_Y_OFFSET);
        if (snapToPixel) {
            tx = Math.round(tx);
            ty = Math.round(ty);
        }

        tr.begin(scale, false, false);
        float cx = tx;
        for (TextRenderUtil.FlatPart part : parts) {
            if (part.text().isEmpty()) continue;
            int color = HudRenderUtil.scaleAlpha(part.color(), alpha);
            tr.render(part.text(), cx, ty, new RenderColor(color), false);
            cx += (float) tr.getWidth(part.text(), false);
        }
        tr.end();
    }

    static float resolveScaleBase() {
        BetterButtons cfg = BetterButtons.get();
        if (cfg != null && cfg.getStyle().get() == BetterButtons.Style.CUSTOM) {
            return TEXT_SCALE_BASE * cfg.getCustomTextScale().get();
        }
        return TEXT_SCALE_BASE;
    }

    static float resolvePaddingX() {
        BetterButtons cfg = BetterButtons.get();
        if (cfg != null && cfg.getStyle().get() == BetterButtons.Style.CUSTOM) {
            return cfg.getCustomTextPadX().get();
        }
        return TEXT_PADDING_X;
    }

    static int resolveTextColor(boolean enabled, float hover) {
        BetterButtons cfg = BetterButtons.get();
        if (cfg != null && cfg.getStyle().get() == BetterButtons.Style.CUSTOM) {
            return enabled ? cfg.getCustomText().getArgb() : cfg.getCustomTextDisabled().getArgb();
        }
        return enabled ? theme().textPrimary() : theme().textMuted();
    }

    private static List<TextRenderUtil.FlatPart> flattenLegacy(Component text, int defaultColor) {
        Component converted = LegacyTextUtil.convertLegacyCodes(text);
        return TextRenderUtil.flatten(converted, defaultColor);
    }

    private static List<TextRenderUtil.FlatPart> trimParts(TextRenderer tr, List<TextRenderUtil.FlatPart> parts, float maxWidth, float scale, int defaultColor) {
        if (parts.isEmpty()) return parts;
        tr.begin(scale, true, false);
        try {
            double total = 0.0;
            for (TextRenderUtil.FlatPart part : parts) {
                if (!part.text().isEmpty()) {
                    total += tr.getWidth(part.text(), false);
                }
            }
            if (total <= maxWidth) return parts;

            String ellipsis = "...";
            double ellipsisWidth = tr.getWidth(ellipsis, false);
            if (maxWidth <= ellipsisWidth) {
                List<TextRenderUtil.FlatPart> out = new ArrayList<>();
                out.add(new TextRenderUtil.FlatPart(ellipsis, defaultColor));
                return out;
            }

            List<TextRenderUtil.FlatPart> out = new ArrayList<>();
            double width = 0.0;
            for (TextRenderUtil.FlatPart part : parts) {
                String s = part.text();
                if (s.isEmpty()) continue;

                double prev = 0.0;
                int keep = 0;
                for (int i = 0; i < s.length(); i++) {
                    double cur = tr.getWidth(s, i + 1, false);
                    double delta = cur - prev;
                    if (width + delta + ellipsisWidth > maxWidth) {
                        if (keep > 0) {
                            out.add(new TextRenderUtil.FlatPart(s.substring(0, keep), part.color()));
                        }
                        out.add(new TextRenderUtil.FlatPart(ellipsis, defaultColor));
                        return out;
                    }
                    width += delta;
                    prev = cur;
                    keep = i + 1;
                }

                out.add(new TextRenderUtil.FlatPart(s.substring(0, keep), part.color()));
            }

            out.add(new TextRenderUtil.FlatPart(ellipsis, defaultColor));
            return out;
        } finally {
            tr.end();
        }
    }

    private static double measurePartsWidth(TextRenderer tr, List<TextRenderUtil.FlatPart> parts, float scale) {
        tr.begin(scale, true, false);
        try {
            double width = 0.0;
            for (TextRenderUtil.FlatPart part : parts) {
                if (!part.text().isEmpty()) {
                    width += tr.getWidth(part.text(), false);
                }
            }
            return width;
        } finally {
            tr.end();
        }
    }

    private static double measureHeight(TextRenderer tr, float scale) {
        tr.begin(scale, true, false);
        try {
            return tr.getHeight(false);
        } finally {
            tr.end();
        }
    }
}
