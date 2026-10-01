package dev.loki.lovisual.features.gui.mainmenu.hero.logic;

/** Axis-aligned rectangle in the fixed logical viewport. */
public record Rect(float x, float y, float w, float h) {
    public boolean contains(float px, float py) {
        return px >= x && px <= x + w && py >= y && py <= y + h;
    }

    public Rect offset(float dx, float dy) {
        return new Rect(x + dx, y + dy, w, h);
    }
}
