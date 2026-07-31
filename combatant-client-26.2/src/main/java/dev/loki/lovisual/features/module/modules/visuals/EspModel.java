/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.render.helpers.ScreenSpaceOverlay2D;
import net.minecraft.world.entity.Entity;

final class EspModel {

    private EspModel() {
    }

    @FunctionalInterface
    interface ColorResolver {
        int colorAt(double x, double y, double boxX1, double boxY1, double boxX2, double boxY2);
    }

    record RenderEntry(Entity entity, int color, double distSq) {
    }

    record SearchLabelEntry(double distSq, ScreenSpaceOverlay2D.ScreenRect rect, int color,
                            ScreenSpaceOverlay2D.LabelEntry label) {
    }

    record ScreenBox(double minX, double minY, double maxX, double maxY) {
    }

    record SolidColor(int color) implements ColorResolver {
        static SolidColor of(int color) {
            return new SolidColor(color);
        }

        @Override
        public int colorAt(double x, double y, double boxX1, double boxY1, double boxX2, double boxY2) {
            return color;
        }
    }

    record Gradient(int topLeft, int topRight, int bottomRight, int bottomLeft) implements ColorResolver {
        private static double safeRatio(double value, double length) {
            if (length <= 0.0001) return 0.0;
            if (value <= 0.0) return 0.0;
            if (value >= length) return 1.0;
            return value / length;
        }

        @Override
        public int colorAt(double x, double y, double boxX1, double boxY1, double boxX2, double boxY2) {
            double tx = safeRatio(x - boxX1, boxX2 - boxX1);
            double ty = safeRatio(y - boxY1, boxY2 - boxY1);

            int top = EspColorUtil.mixArgb(topLeft, topRight, (float) tx);
            int bottom = EspColorUtil.mixArgb(bottomLeft, bottomRight, (float) tx);
            return EspColorUtil.mixArgb(top, bottom, (float) ty);
        }
    }
}
