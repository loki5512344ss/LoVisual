package dev.loki.lovisual.features.module.modules.visuals.scene.visual.chams;

public final class ChamsModeHelper {
    private ChamsModeHelper() {}

    public static boolean isSmokeMode(String mode) { return "Smoke".equals(mode); }
    public static boolean isMetallicMode(String mode) { return "Metallic".equals(mode); }
    public static boolean isNoiseMode(String mode) { return "Noise".equals(mode); }
    public static boolean isGlassMode(String mode) { return "Glass".equals(mode); }
    public static boolean isSnowMode(String mode) { return "Snow".equals(mode); }
    public static boolean isChromaMode(String mode) { return "Chroma".equals(mode); }
    public static boolean isSolidMode(String mode) { return "Solid".equals(mode); }
    public static boolean isStripesMode(String mode) { return "Stripes".equals(mode); }
    public static boolean isGlowShaderMode(String mode) { return "Glow".equals(mode); }
    public static boolean isBalatroMode(String mode) { return "Balatro".equals(mode); }

    public static boolean isHandEffectMode(String mode) {
        return isSmokeMode(mode) || isMetallicMode(mode) || isNoiseMode(mode);
    }

    public static boolean isGlowVisible(String mode, boolean glow) {
        return isHandEffectMode(mode) && glow;
    }

    public static boolean isShadowVisible(String mode, boolean shadow) {
        return isHandEffectMode(mode) && shadow;
    }

    public static boolean isCustomEffectColor(String mode, String effectColorSource) {
        return isHandEffectMode(mode) && "Custom".equals(effectColorSource);
    }

    public static boolean isCustomMetallicVisualColor(String mode, String visualColorSource) {
        return isMetallicMode(mode) && "Custom".equals(visualColorSource);
    }

    public static int blendModeIndex(String blendMode) {
        return switch (blendMode) {
            case "Add" -> 1;
            case "Multiply" -> 2;
            case "Screen" -> 3;
            case "Overlay" -> 4;
            case "Difference" -> 5;
            default -> 0;
        };
    }
}
