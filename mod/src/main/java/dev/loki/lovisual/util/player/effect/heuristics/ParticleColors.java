package dev.loki.lovisual.util.player.effect.heuristics;

import dev.loki.lovisual.mixins.accessors.state.EffectParticleEffectAccessor;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.List;

/**
 * Palette-independent particle color utilities: pulling an RGB color (or a directly-tagged effect)
 * out of a {@link ParticleOptions}, brightness normalization, HSV hue and the palette color arrays
 * derived from a set of mob effects. Used by {@link ParticlePalette} during inference.
 */
final class ParticleColors {
    private ParticleColors() {
    }

    static Holder<MobEffect> directEffectFromParticle(ParticleOptions particle) {
        if (particle == ParticleTypes.RAID_OMEN) return MobEffects.RAID_OMEN;
        if (particle == ParticleTypes.TRIAL_OMEN) return MobEffects.TRIAL_OMEN;
        return null;
    }

    static int extractColor(ParticleOptions particle) {
        if (particle instanceof EffectParticleEffectAccessor effect) {
            return effect.lovisual$getColor() & 0xFFFFFF;
        }
        if (particle instanceof ColorParticleOption tinted) {
            int r = Math.min(255, Math.max(0, Math.round(tinted.getRed() * 255f)));
            int g = Math.min(255, Math.max(0, Math.round(tinted.getGreen() * 255f)));
            int b = Math.min(255, Math.max(0, Math.round(tinted.getBlue() * 255f)));
            return (r << 16) | (g << 8) | b;
        }
        return 0;
    }

    static int[] computeColors(List<Holder<MobEffect>> palette) {
        int[] colors = new int[palette.size()];
        for (int i = 0; i < palette.size(); i++) {
            colors[i] = palette.get(i).value().getColor();
        }
        return colors;
    }

    static int[] computeNormalizedColors(int[] colors) {
        int[] normalized = new int[colors.length];
        for (int i = 0; i < colors.length; i++) {
            normalized[i] = normalizeColor(colors[i]);
        }
        return normalized;
    }

    static int normalizeColor(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int max = Math.max(r, Math.max(g, b));
        if (max <= 0) return 0;
        float scale = 255f / (float) max;
        int nr = Math.min(255, Math.round(r * scale));
        int ng = Math.min(255, Math.round(g * scale));
        int nb = Math.min(255, Math.round(b * scale));
        return (nr << 16) | (ng << 8) | nb;
    }

    // Basic HSV hue calculation (degrees 0-360)
    static float hueDegrees(int r, int g, int b) {
        float rf = r / 255f;
        float gf = g / 255f;
        float bf = b / 255f;
        float max = Math.max(rf, Math.max(gf, bf));
        float min = Math.min(rf, Math.min(gf, bf));
        float delta = max - min;
        if (delta <= 0f) return 0f;
        float hue;
        if (max == rf) {
            hue = (gf - bf) / delta;
        } else if (max == gf) {
            hue = 2f + (bf - rf) / delta;
        } else {
            hue = 4f + (rf - gf) / delta;
        }
        hue *= 60f;
        if (hue < 0f) hue += 360f;
        return hue;
    }
}
