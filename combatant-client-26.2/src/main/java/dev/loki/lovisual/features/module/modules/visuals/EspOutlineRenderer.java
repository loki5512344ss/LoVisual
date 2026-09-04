/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.render.engine.renderer.Renderer2D;

final class EspOutlineRenderer {

    private EspOutlineRenderer() {
    }

    static void drawOutline(ESP esp, Renderer2D renderer, double x1, double y1, double x2, double y2,
                            double thickness, EspModel.ColorResolver colors) {
        double width = x2 - x1;
        double height = y2 - y1;
        if (width <= 0 || height <= 0 || thickness <= 0.0) return;

        if (esp.isCornerBox()) {
            drawOutlineCorners(esp, renderer, x1, y1, x2, y2, thickness, colors);
            return;
        }

        if (esp.isChamferedBox()) {
            drawOutlineChamfered(esp, renderer, x1, y1, x2, y2, thickness, colors);
            return;
        }

        drawGradientQuad(renderer, x1, y1, x2, y1 + thickness, x1, y1, x2, y2, colors);
        drawGradientQuad(renderer, x1, y2 - thickness, x2, y2, x1, y1, x2, y2, colors);
        drawGradientQuad(renderer, x1, y1, x1 + thickness, y2, x1, y1, x2, y2, colors);
        drawGradientQuad(renderer, x2 - thickness, y1, x2, y2, x1, y1, x2, y2, colors);
    }

    private static void drawOutlineChamfered(ESP esp, Renderer2D renderer, double x1, double y1, double x2, double y2,
                                             double thickness, EspModel.ColorResolver colors) {
        double width = x2 - x1;
        double height = y2 - y1;
        if (width <= 0.0 || height <= 0.0) return;

        double chamfer = Math.min(width, height) * ESP.CHAMFER_FRACTION;
        chamfer = Math.max(chamfer, thickness * 3.0);
        chamfer = Math.min(chamfer, Math.min(width, height) * 0.32);
        if (chamfer <= thickness) {
            drawGradientQuad(renderer, x1, y1, x2, y1 + thickness, x1, y1, x2, y2, colors);
            drawGradientQuad(renderer, x1, y2 - thickness, x2, y2, x1, y1, x2, y2, colors);
            drawGradientQuad(renderer, x1, y1, x1 + thickness, y2, x1, y1, x2, y2, colors);
            drawGradientQuad(renderer, x2 - thickness, y1, x2, y2, x1, y1, x2, y2, colors);
            return;
        }

        renderer.chamferedRectStrokeQuad(
                x1,
                y1,
                width,
                height,
                chamfer,
                chamfer,
                chamfer,
                chamfer,
                thickness,
                colors.colorAt(x1, y1, x1, y1, x2, y2),
                colors.colorAt(x2, y1, x1, y1, x2, y2),
                colors.colorAt(x2, y2, x1, y1, x2, y2),
                colors.colorAt(x1, y2, x1, y1, x2, y2)
        );
    }

    private static void drawOutlineCorners(ESP esp, Renderer2D renderer, double x1, double y1, double x2, double y2,
                                           double thickness, EspModel.ColorResolver colors) {
        double width = x2 - x1;
        double height = y2 - y1;
        if (width <= 0 || height <= 0) return;

        double seg = Math.min(width, height) * ESP.CORNER_SEGMENT_FRACTION;
        double minSeg = thickness * 2.0;
        double segX = Math.min(width * 0.5, Math.max(seg, minSeg));
        double segY = Math.min(height * 0.5, Math.max(seg, minSeg));

        drawCornerSegment(renderer, x1, y1, x1 + segX, y1 + thickness, x1, y1, x2, y2, colors);
        drawCornerSegment(renderer, x1, y1, x1 + thickness, y1 + segY, x1, y1, x2, y2, colors);

        drawCornerSegment(renderer, x2 - segX, y1, x2, y1 + thickness, x1, y1, x2, y2, colors);
        drawCornerSegment(renderer, x2 - thickness, y1, x2, y1 + segY, x1, y1, x2, y2, colors);

        drawCornerSegment(renderer, x1, y2 - thickness, x1 + segX, y2, x1, y1, x2, y2, colors);
        drawCornerSegment(renderer, x1, y2 - segY, x1 + thickness, y2, x1, y1, x2, y2, colors);

        drawCornerSegment(renderer, x2 - segX, y2 - thickness, x2, y2, x1, y1, x2, y2, colors);
        drawCornerSegment(renderer, x2 - thickness, y2 - segY, x2, y2, x1, y1, x2, y2, colors);
    }

    private static void drawCornerSegment(Renderer2D renderer,
                                          double sx1, double sy1, double sx2, double sy2,
                                          double boxX1, double boxY1, double boxX2, double boxY2,
                                          EspModel.ColorResolver colors) {
        drawGradientQuad(renderer, sx1, sy1, sx2, sy2, boxX1, boxY1, boxX2, boxY2, colors);
    }

    private static void drawGradientQuad(Renderer2D renderer,
                                         double sx1, double sy1, double sx2, double sy2,
                                         double boxX1, double boxY1, double boxX2, double boxY2,
                                         EspModel.ColorResolver colors) {
        renderer.quad(
                sx1, sy1, sx2 - sx1, sy2 - sy1,
                colors.colorAt(sx1, sy1, boxX1, boxY1, boxX2, boxY2),
                colors.colorAt(sx2, sy1, boxX1, boxY1, boxX2, boxY2),
                colors.colorAt(sx2, sy2, boxX1, boxY1, boxX2, boxY2),
                colors.colorAt(sx1, sy2, boxX1, boxY1, boxX2, boxY2)
        );
    }
}
