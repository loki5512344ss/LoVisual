package dev.loki.lovisual.features.gui.mainmenu.hero.logic;

/** Measures text in logical pixels so the layout can be built from real glyph sizes. */
public interface TextMetrics {
    /** Line height of the bold UI font at {@code px} logical pixels. */
    float height(float px);
}
