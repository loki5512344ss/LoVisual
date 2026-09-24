package dev.loki.lovisual.features.gui.hud.draggable.impl;

import dev.loki.lovisual.config.values.mode.ModeValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;

public enum HudPanelLayoutModes {
    ;
    public static final String SPLIT_HEADER = "Split Header";
    public static final String UNIFIED_DIVIDER = "Unified Divider";

    public static final double SCALE_DEFAULT = 1.0;
    public static final double SCALE_MIN = 1.0;
    public static final double SCALE_MAX = 1.4;
    public static final float SCALE_BASE_MULTIPLIER = 2.0f;

    public static float effectiveScale(NumberValue<Double> value) {
        if (value == null || value.get() == null) {
            return SCALE_BASE_MULTIPLIER;
        }
        return value.get().floatValue() * SCALE_BASE_MULTIPLIER;
    }

    public static String current(ModeValue value) {
        String mode = value != null ? value.get() : null;
        return UNIFIED_DIVIDER.equals(mode) ? UNIFIED_DIVIDER : SPLIT_HEADER;
    }
}
