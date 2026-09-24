package dev.loki.lovisual.features.module.modules.visuals.scene.visual;

import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.config.values.primitive.BooleanMapValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.theme.Theme;
import java.util.Map;

final class ReimaginedSkyboxHelper {
    private final BooleanValue syncTheme;
    private final RGBColorValue color;
    private final NumberValue<Float> motionSpeed;
    private final BooleanValue auroraEnabled;
    private final NumberValue<Float> auroraIntensity;
    private final NumberValue<Float> auroraSpeed;
    private final NumberValue<Float> smallStars;
    private final NumberValue<Float> dustStars;
    private final NumberValue<Float> mediumStars;
    private final NumberValue<Float> largeStars;
    private final NumberValue<Float> starBrightness;
    private final NumberValue<Float> twinkleStrength;
    private final BooleanMapValue layers;
    private final NumberValue<Float> skyFogBlend;
    private final Map<String, Boolean> defaultLayers;

    ReimaginedSkyboxHelper(
            BooleanValue syncTheme,
            RGBColorValue color,
            NumberValue<Float> motionSpeed,
            BooleanValue auroraEnabled,
            NumberValue<Float> auroraIntensity,
            NumberValue<Float> auroraSpeed,
            NumberValue<Float> smallStars,
            NumberValue<Float> dustStars,
            NumberValue<Float> mediumStars,
            NumberValue<Float> largeStars,
            NumberValue<Float> starBrightness,
            NumberValue<Float> twinkleStrength,
            BooleanMapValue layers,
            NumberValue<Float> skyFogBlend,
            Map<String, Boolean> defaultLayers) {
        this.syncTheme = syncTheme;
        this.color = color;
        this.motionSpeed = motionSpeed;
        this.auroraEnabled = auroraEnabled;
        this.auroraIntensity = auroraIntensity;
        this.auroraSpeed = auroraSpeed;
        this.smallStars = smallStars;
        this.dustStars = dustStars;
        this.mediumStars = mediumStars;
        this.largeStars = largeStars;
        this.starBrightness = starBrightness;
        this.twinkleStrength = twinkleStrength;
        this.layers = layers;
        this.skyFogBlend = skyFogBlend;
        this.defaultLayers = defaultLayers;
    }

    int getShaderSkyboxColor() {
        if (syncTheme.get()) {
            return Theme.theme().accent() & 0x00FFFFFF;
        }
        return color.getArgb() & 0x00FFFFFF;
    }

    float getSkyboxSkyFogBlend() {
        return clamp01(skyFogBlend.get());
    }

    float getSkyboxShaderMotionSpeed() {
        return clamp(motionSpeed.get(), 0.0f, 2.0f);
    }

    boolean isSkyboxShaderAuroraEnabled() {
        return auroraEnabled.get();
    }

    float getSkyboxShaderAuroraIntensity() {
        if (!auroraEnabled.get()) {
            return 0.0f;
        }
        return clamp(auroraIntensity.get(), 0.0f, 1.5f);
    }

    float getSkyboxShaderAuroraSpeed() {
        return clamp(auroraSpeed.get(), 0.0f, 2.0f);
    }

    float getSmallStars() {
        return clampStar(smallStars.get());
    }

    float getDustStars() {
        return clampStar(dustStars.get());
    }

    float getMediumStars() {
        return clampStar(mediumStars.get());
    }

    float getLargeStars() {
        return clampStar(largeStars.get());
    }

    float getStarBrightness() {
        return clamp(starBrightness.get(), 0.0f, 3.0f);
    }

    float getTwinkleStrength() {
        return clamp(twinkleStrength.get(), 0.0f, 3.0f);
    }

    int getLayerMask() {
        int mask = 0;
        int bit = 0;
        for (String key : defaultLayers.keySet()) {
            if (layers.get(key)) {
                mask |= 1 << bit;
            }
            bit++;
        }
        return mask;
    }

    private float clampStar(float value) {
        return Math.max(0.0f, Math.min(10.0f, value));
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
