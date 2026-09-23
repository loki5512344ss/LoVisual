/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.glyph;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.text.font.Font;
import dev.loki.lovisual.render.engine.text.glyph.custom.HorizontalFade;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

/**
 * Градиентные текстовые прогоны {@link CustomTextRenderer}: линейный градиент,
 * квад-градиент и горизонтальный fade (опционально под scissor-клипом).
 * Тела 1:1 с прежними методами CustomTextRenderer.
 */
final class GradientTexts {
    private GradientTexts() {
    }

    static double gradient(CustomTextRenderer r, String text, double x, double y,
                           Font.GlyphGradient gradient, boolean shadow) {
        return r.autoRun(() -> {
            r.ensureMeshStarted();
            if (shadow) {
                return r.shadowPass(null, false,
                        shadowGlyphs(r, text, x, y),
                        () -> r.font.renderGradient(r.mesh, text, x, y, r.scale / 1.5, gradient));
            }
            return r.font.renderGradient(r.mesh, text, x, y, r.scale / 1.5, gradient);
        });
    }

    static double quadGradient(CustomTextRenderer r, String text, double x, double y,
                               Font.GlyphQuadGradient gradient, boolean shadow) {
        return r.autoRun(() -> {
            r.ensureMeshStarted();
            if (shadow) {
                return r.shadowPass(null, false,
                        shadowGlyphs(r, text, x, y),
                        () -> r.font.renderQuadGradient(r.mesh, text, x, y, r.scale / 1.5, gradient));
            }
            return r.font.renderQuadGradient(r.mesh, text, x, y, r.scale / 1.5, gradient);
        });
    }

    static double horizontalFade(CustomTextRenderer r, String text, double x, double y, RenderColor color,
                                 double clipLeft, double clipRight, double fadeLeft, double fadeRight,
                                 boolean shadow) {
        return r.autoRun(() -> {
            r.ensureMeshStarted();
            double glyphScale = r.scale / 1.5;
            Font.GlyphGradient mainGradient =
                    HorizontalFade.gradient(r.font, color, clipLeft, clipRight, fadeLeft, fadeRight, glyphScale);
            if (shadow) {
                Font.GlyphGradient shadowGradient =
                        HorizontalFade.gradient(r.font, CustomTextRenderer.SHADOW_COLOR, clipLeft, clipRight, fadeLeft, fadeRight, glyphScale);
                return r.shadowPass(color, false,
                        () -> r.font.renderGradient(r.mesh, text, x + r.fontScale * glyphScale, y + r.fontScale * glyphScale, glyphScale, shadowGradient),
                        () -> r.font.renderGradient(r.mesh, text, x, y, glyphScale, mainGradient));
            }
            return r.font.renderGradient(r.mesh, text, x, y, glyphScale, mainGradient);
        });
    }

    static double horizontalFadeClipped(CustomTextRenderer r, String text, double x, double y, RenderColor color,
                                        double clipLeft, double clipRight, double fadeLeft, double fadeRight,
                                        boolean shadow) {
        boolean wasBuilding = r.building;
        if (!wasBuilding) r.begin();
        boolean clipped = false;
        try {
            double clipWidth = Math.max(0.0, clipRight - clipLeft);
            double clipHeight = Math.max(0.0, r.getHeight(shadow));
            clipped = ScissorFunction.pushRaw((float) clipLeft, (float) y, (float) clipWidth, (float) clipHeight);
            return horizontalFade(r, text, x, y, color, clipLeft, clipRight, fadeLeft, fadeRight, shadow);
        } finally {
            if (clipped) {
                ScissorFunction.pop();
            }
            if (!wasBuilding) {
                r.end();
            }
        }
    }

    private static CustomTextRenderer.GlyphRun shadowGlyphs(CustomTextRenderer r, String text, double x, double y) {
        double offset = r.fontScale * r.scale / 1.5;
        double glyphScale = r.scale / 1.5;
        return () -> r.font.render(r.mesh, text, x + offset, y + offset, CustomTextRenderer.SHADOW_COLOR, glyphScale);
    }
}
