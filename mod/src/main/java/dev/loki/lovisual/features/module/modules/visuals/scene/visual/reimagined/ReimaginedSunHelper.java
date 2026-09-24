package dev.loki.lovisual.features.module.modules.visuals.scene.visual;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;

final class ReimaginedSunHelper {
    private final BooleanValue glareEnabled;
    private final NumberValue<Float> size;
    private final NumberValue<Float> glow;
    private final NumberValue<Float> intensity;

    ReimaginedSunHelper(
            BooleanValue glareEnabled,
            NumberValue<Float> size,
            NumberValue<Float> glow,
            NumberValue<Float> intensity) {
        this.glareEnabled = glareEnabled;
        this.size = size;
        this.glow = glow;
        this.intensity = intensity;
    }

    boolean isGlareEnabled() {
        return glareEnabled.get();
    }

    float getSize() {
        return size.get();
    }

    float getGlow() {
        return glow.get();
    }

    float getIntensity() {
        return intensity.get();
    }
}
