package dev.loki.lovisual.features.module.modules.visuals.effects.ribbon;

/** Точка следа: позиция в мире, время (мс) появления и скорость источника в этот момент. */
public record RibbonPoint(double x, double y, double z, long ms, float speed) {

    public RibbonPoint(double x, double y, double z, long ms) {
        this(x, y, z, ms, 0.0f);
    }
}
