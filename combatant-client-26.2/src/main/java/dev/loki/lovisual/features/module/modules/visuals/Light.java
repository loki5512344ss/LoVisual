/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.config.values.BooleanValue;
import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.config.values.RGBColorValue;
import dev.loki.lovisual.events.EventHandler;
import dev.loki.lovisual.events.impl.LightmapModifyEvent;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Tints the world ambient light with a custom color. Pure math lives in
 * {@link #tintAmbient} so it is unit-testable without Minecraft.
 */
@ModuleInfo(
        id = "light",
        displayName = "Light",
        category = ModuleCategory.VISUALS
)
public class Light extends Module {

    private static final String SETTING_TINT_AMBIENT = "tint_ambient";
    private static final String SETTING_TINT_SKY = "tint_sky";
    private static final String SETTING_COLOR = "color";
    private static final String SETTING_STRENGTH = "strength";

    private final BooleanValue tintAmbientSetting = bool("lightTintAmbient", SETTING_TINT_AMBIENT, true);
    private final BooleanValue tintSkySetting = bool("lightTintSky", SETTING_TINT_SKY, false);
    private final RGBColorValue color = colorNoAlpha("lightColor", SETTING_COLOR, "#C7D9FF");
    private final NumberValue<Integer> strength = num("lightStrength", SETTING_STRENGTH, 50, 0, 100);

    @EventHandler
    private void onLightmapState(LightmapModifyEvent event) {
        if (!isEnabled() || event.state() == null) return;

        float mix = clamp01(strength.get() / 100.0f);
        if (mix <= 0.0f) return;

        int argb = color.getArgb();
        Vector3f target = toLinear(argb);

        var state = event.state();
        if (tintAmbientSetting.get()) {
            state.ambientColor = tintAmbient(state.ambientColor, target, mix);
        }
        if (tintSkySetting.get()) {
            state.skyLightColor = tintAmbient(state.skyLightColor, target, mix);
        }
        state.needsUpdate = true;
    }

    /**
     * @param current existing channel (may be null)
     * @param target  replacement linear RGB
     * @param mix     0..1 blend factor; 0 keeps current, 1 is fully target
     */
    public static Vector3f tintAmbient(Vector3fc current, Vector3f target, float mix) {
        float k = clamp01(mix);
        Vector3f base = current == null ? new Vector3f(1.0f, 1.0f, 1.0f)
                : new Vector3f(current.x(), current.y(), current.z());
        base.x = base.x * (1.0f - k) + target.x * k;
        base.y = base.y * (1.0f - k) + target.y * k;
        base.z = base.z * (1.0f - k) + target.z * k;
        return base;
    }

    /** Converts 0xRRGGBB (alpha ignored) into a linear-ish 0..1 tint vector. */
    public static Vector3f toLinear(int argb) {
        return new Vector3f(
                ((argb >> 16) & 0xFF) / 255.0f,
                ((argb >> 8) & 0xFF) / 255.0f,
                (argb & 0xFF) / 255.0f
        );
    }

    private static float clamp01(float v) {
        return v < 0.0f ? 0.0f : Math.min(v, 1.0f);
    }
}
