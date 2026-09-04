/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.util.time.GameClock;

import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import net.minecraft.world.phys.AABB;

import java.util.Locale;

final class EspColorUtil {

    private EspColorUtil() {
    }

    static int boxGradientColor(int baseRgb, AABB box, double x, double y, double z, int alpha, float strength) {
        double nx = safeRatio(x - box.minX, box.maxX - box.minX);
        double ny = safeRatio(y - box.minY, box.maxY - box.minY);
        double nz = safeRatio(z - box.minZ, box.maxZ - box.minZ);
        double phase = AnimationUtility.time(ESP.BOX_3D_GRADIENT_SPEED)
                + (box.minX + box.minY * 0.7 + box.minZ * 1.3) * 0.18;
        double waveA = 0.5 + 0.5 * Math.sin(phase + nx * 2.65 + ny * 5.30 - nz * 1.75);
        double waveB = 0.5 + 0.5 * Math.sin(phase * 0.72 - nx * 3.10 + ny * 1.35 + nz * 4.40);
        float t = clamp01((float) (ny * 0.44 + nz * 0.18 + (1.0 - nx) * 0.16 + waveA * 0.16 + waveB * 0.12));

        int darkRgb = mixRgb(baseRgb, 0x000000, clamp01(0.18f + 0.36f * strength));
        int lightRgb = mixRgb(baseRgb, 0xFFFFFF, clamp01(0.16f + 0.40f * strength));
        int gradientRgb = mixRgb(darkRgb, lightRgb, t);
        int preservedRgb = mixRgb(baseRgb, gradientRgb, clamp01(strength));
        return withAlpha(preservedRgb, alpha);
    }

    static double safeRatio(double value, double length) {
        if (length <= 0.0001) return 0.0;
        if (value <= 0.0) return 0.0;
        if (value >= length) return 1.0;
        return value / length;
    }

    static String formatHealth(float health) {
        if (Math.abs(health - Math.round(health)) < 0.05f) {
            return Math.round(health) + " HP";
        }
        return String.format(Locale.ROOT, "%.1f HP", health);
    }

    static float shaderSmokeTime() {
        return (GameClock.millis() % 600_000L) / 1000.0f;
    }

    static int deriveSmokeColor(int rgb, int layer) {
        int base = rgb & 0x00FFFFFF;
        return switch (layer) {
            case 0 -> mixRgb(base, 0xFFFFFF, 0.18f);
            case 1 -> base;
            default -> mixRgb(base, 0x05070C, 0.48f);
        };
    }

    static int withAlpha(int rgb, int alpha) {
        int a = clamp(alpha);
        return (a << 24) | (rgb & 0x00FFFFFF);
    }

    static int alpha255(float alpha) {
        return clamp(Math.round(clamp01(alpha) * 255.0f));
    }

    static int mixRgb(int rgbA, int rgbB, float t) {
        float clamped = Math.max(0f, Math.min(1f, t));
        int ar = (rgbA >>> 16) & 0xFF;
        int ag = (rgbA >>> 8) & 0xFF;
        int ab = rgbA & 0xFF;
        int br = (rgbB >>> 16) & 0xFF;
        int bg = (rgbB >>> 8) & 0xFF;
        int bb = rgbB & 0xFF;

        int r = Math.round(ar + (br - ar) * clamped);
        int g = Math.round(ag + (bg - ag) * clamped);
        int b = Math.round(ab + (bb - ab) * clamped);
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    static int scaleRgb(int rgb, float multiplier) {
        float m = Math.max(0.0f, Math.min(2.0f, multiplier));
        int r = Math.round(((rgb >>> 16) & 0xFF) * m);
        int g = Math.round(((rgb >>> 8) & 0xFF) * m);
        int b = Math.round((rgb & 0xFF) * m);
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    static float clamp01(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    static int healthRgb(float ratio) {
        float t = clamp01(ratio);
        if (t >= 0.5f) {
            float k = (t - 0.5f) / 0.5f;
            return mixRgb(0xFFFF00, 0x00FF00, k);
        }
        float k = t / 0.5f;
        return mixRgb(0xFF0000, 0xFFFF00, k);
    }

    static int clamp(int v) {
        if (v < 0) return 0;
        if (v > 255) return 255;
        return v;
    }

    static int mixArgb(int a, int b, float t) {
        float clamped = clamp01(t);
        int aa = (a >>> 24) & 0xFF;
        int ar = (a >>> 16) & 0xFF;
        int ag = (a >>> 8) & 0xFF;
        int ab = a & 0xFF;
        int ba = (b >>> 24) & 0xFF;
        int br = (b >>> 16) & 0xFF;
        int bg = (b >>> 8) & 0xFF;
        int bb = b & 0xFF;

        int oa = Math.round(aa + (ba - aa) * clamped);
        int or = Math.round(ar + (br - ar) * clamped);
        int og = Math.round(ag + (bg - ag) * clamped);
        int ob = Math.round(ab + (bb - ab) * clamped);
        return (clamp(oa) << 24) | (clamp(or) << 16) | (clamp(og) << 8) | clamp(ob);
    }
}
