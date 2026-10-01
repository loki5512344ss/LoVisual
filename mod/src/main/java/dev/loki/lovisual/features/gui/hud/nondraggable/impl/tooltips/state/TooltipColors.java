package dev.loki.lovisual.features.gui.hud.nondraggable.impl.tooltips;

/** Keeps vanilla tooltip text readable on the themed panel. */
final class TooltipColors {
    private static final int VANILLA_BLUE = 0x5555FF;
    private static final int VANILLA_DARK_BLUE = 0x0000AA;

    private TooltipColors() {
    }

    /**
     * Vanilla paints creative-tab lines pure blue, which is hard to read on a dark themed panel;
     * those two shades are replaced by the theme accent, every other color is kept.
     */
    static int readable(int argb, int accentArgb) {
        int rgb = argb & 0x00FFFFFF;
        if (rgb == VANILLA_BLUE || rgb == VANILLA_DARK_BLUE) {
            return 0xFF000000 | (accentArgb & 0x00FFFFFF);
        }
        return argb;
    }
}
