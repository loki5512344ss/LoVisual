package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;

import static dev.loki.lovisual.features.theme.Theme.theme;

final class ModuleListPalette {

    private ModuleListPalette() {}

    static boolean isThemeMode(ModuleList host) {
        return host.colorMode.get() == ModuleList.ColorMode.THEME;
    }

    static boolean isCustomMode(ModuleList host) {
        return host.colorMode.get() == ModuleList.ColorMode.CUSTOM;
    }

    static boolean isBlurEffect(ModuleList host) {
        return host.bgEffect.get() == ModuleList.BgEffect.BLUR;
    }

    static boolean isGlassEffect(ModuleList host) {
        return host.bgEffect.get() == ModuleList.BgEffect.GLASS;
    }

    static boolean hasEffect(ModuleList host) {
        return host.bgEffect.get() != ModuleList.BgEffect.NONE;
    }

    static boolean hasAnimatedTextGradient(ModuleList host) {
        return host.textGradientMode.get() != AnimatedRenderColors.Mode.STATIC
                || (isCustomMode(host) && (host.textTopColor.isRainbow() || host.textBottomColor.isRainbow()));
    }

    static int resolveBgTop(ModuleList host) {
        if (isThemeMode(host)) {
            int base = HudRenderUtil.mixColor(theme().windowBg(), 0xFF000000, 0.38f);
            return HudRenderUtil.setAlpha(base, host.bgAlpha.get());
        }
        return host.bgTopColor.getArgb();
    }

    static int resolveBgBottom(ModuleList host) {
        if (isThemeMode(host)) {
            int base = HudRenderUtil.mixColor(theme().windowHeader(), 0xFF000000, 0.48f);
            return HudRenderUtil.setAlpha(base, host.bgAlpha.get());
        }
        return host.bgBottomColor.getArgb();
    }

    static int resolveTextTop(ModuleList host) {
        if (isThemeMode(host)) {
            return HudRenderUtil.mixColor(theme().accentSoft(), theme().accent(), 0.18f);
        }
        return host.textTopColor.getArgb();
    }

    static int resolveTextBottom(ModuleList host) {
        if (isThemeMode(host)) {
            return theme().accent();
        }
        return host.textBottomColor.getArgb();
    }

    static int resolveTextGradientTop(ModuleList host) {
        if (isThemeMode(host)) return resolveTextTop(host);
        return host.textTopColor.isRainbow()
                ? parseRgbFallback(host.textTopColor.rainbowFallbackHex()) : host.textTopColor.getArgb();
    }

    static int resolveTextGradientBottom(ModuleList host) {
        if (isThemeMode(host)) return resolveTextBottom(host);
        return host.textBottomColor.isRainbow()
                ? parseRgbFallback(host.textBottomColor.rainbowFallbackHex()) : host.textBottomColor.getArgb();
    }

    static int parseRgbFallback(String hex) {
        if (hex == null || hex.isBlank()) return 0xFFFFFFFF;
        String s = hex.trim();
        if (s.startsWith("#")) s = s.substring(1);
        if (s.startsWith("0x") || s.startsWith("0X")) s = s.substring(2);
        try {
            int value = (int) Long.parseUnsignedLong(s, 16);
            if (s.length() <= 6) return 0xFF000000 | (value & 0x00FFFFFF);
            return value;
        } catch (NumberFormatException ignored) {
            return 0xFFFFFFFF;
        }
    }

    static int rowFill(ModuleList host, int argb, int index, int total) {
        if (!isThemeMode(host) || total <= 1) return argb;
        float progress = (float) index / (float) (total - 1);
        int alpha = (argb >>> 24) & 0xFF;
        int rgb = HudRenderUtil.mixColor(argb, resolveTextBottom(host), 0.03f + 0.07f * progress);
        return (rgb & 0x00FFFFFF) | (alpha << 24);
    }

    static void drawGlass(ModuleList host, float x, float y, float w, float h, float radius) {
        float blurStrength = host.blurAlpha.get() / 255f;
        if (blurStrength <= 0.001f) return;
        Renderer2D.COLOR.liquidGlassRect(
                x, y, w, h, radius,
                0xFFFFFFFF,
                1.0f,
                blurStrength,
                Renderer2D.LiquidGlassPreset.BALANCED
        );
    }
}
