package dev.loki.lovisual.features.module.modules.visuals.esp;

import dev.loki.lovisual.render.helpers.clip.ScreenSpaceOverlay2D;
import net.minecraft.world.entity.Entity;
import dev.loki.lovisual.features.module.modules.visuals.esp.EspColorUtil;

public final class EspModel {

    private EspModel() {
    }

    @FunctionalInterface
    public interface ColorResolver {
        int colorAt(double x, double y, double boxX1, double boxY1, double boxX2, double boxY2);
    }

    public record RenderEntry(Entity entity, int color, double distSq) {
    }

    public record SearchLabelEntry(double distSq, ScreenSpaceOverlay2D.ScreenRect rect, int color,
                            ScreenSpaceOverlay2D.LabelEntry label) {
    }

    public record ScreenBox(double minX, double minY, double maxX, double maxY) {
    }

    public record SolidColor(int color) implements ColorResolver {
        public static SolidColor of(int color) {
            return new SolidColor(color);
        }

        @Override
        public int colorAt(double x, double y, double boxX1, double boxY1, double boxX2, double boxY2) {
            return color;
        }
    }

    public record Gradient(int topLeft, int topRight, int bottomRight, int bottomLeft) implements ColorResolver {
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
