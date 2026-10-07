package dev.loki.lovisual.features.gui.hud.draggable.quick;

/** Maps a mouse position on a slider track to a setting value, and back. */
final class QuickSliderMath {
    private QuickSliderMath() {
    }

    static double ratio(double value, double min, double max) {
        return max <= min ? 0.0 : Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    /** Value under the cursor: whole numbers for integer settings, hundredths otherwise. */
    static double valueAt(float mouseX, float trackX, float trackW, double min, double max, boolean integer) {
        double t = trackW <= 0f ? 0.0 : Math.max(0.0, Math.min(1.0, (mouseX - trackX) / trackW));
        double raw = min + (max - min) * t;
        return integer ? Math.round(raw) : Math.round(raw * 100.0) / 100.0;
    }
}
