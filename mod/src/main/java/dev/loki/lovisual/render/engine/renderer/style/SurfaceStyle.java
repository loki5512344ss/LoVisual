package dev.loki.lovisual.render.engine.renderer.style;

import dev.loki.lovisual.config.values.mode.EnumValue;

/**
 * Material used to draw an element background plate. Independent from the
 * theme palette (colors) and from shape geometry (radii / strokes): it only
 * answers "how is the surface drawn". {@link #AUTO} means "follow the global
 * selection" and is only meaningful as a per-widget override.
 */
public enum SurfaceStyle implements EnumValue.IdProvider {
    AUTO("auto"),
    GLASS("glass"),
    BLUR("blur"),
    FLAT("flat"),
    OUTLINE("outline"),
    NONE("none");

    private final String id;

    SurfaceStyle(String id) {
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    /** Global style settings never offer AUTO; it is an override-only value. */
    public boolean isOverrideable() {
        return this != AUTO;
    }
}
