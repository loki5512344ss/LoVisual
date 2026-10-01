package dev.loki.lovisual.features.module.modules.visuals.player.jumpcircles.shape;

/** Ground-plane shapes JumpCircles can draw; flags tell which tuning parameters a shape honours. */
public enum Shape {
    STAR(false, false),
    DIAMOND(false, false),
    SPIRAL(true, true),
    HEXAGON(true, false),
    WAVE(true, true),
    CROSS(false, false),
    PETALS(false, true);

    private final boolean usesThickness;
    private final boolean usesSegments;

    Shape(boolean usesThickness, boolean usesSegments) {
        this.usesThickness = usesThickness;
        this.usesSegments = usesSegments;
    }

    public boolean usesThickness() {
        return usesThickness;
    }

    public boolean usesSegments() {
        return usesSegments;
    }
}
