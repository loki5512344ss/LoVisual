/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals;

import dev.loki.lovisual.config.values.NumberValue;
import dev.loki.lovisual.config.values.RGBColorValue;
import dev.loki.lovisual.config.values.ModeValue;
import dev.loki.lovisual.features.module.Module;
import dev.loki.lovisual.features.module.ModuleCategory;
import dev.loki.lovisual.features.module.ModuleInfo;
import dev.loki.lovisual.features.module.Modules;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;

/**
 * Атмосферный туман зари: плотность тумана и цвет неба зависят от режима
 * (рассвет/сумерки), времени суток, высоты камеры и мягкости градиента.
 * В отличие от референса здесь эффект реально применяется к FogData и цвету.
 */
@ModuleInfo(
        id = "atmodawnfog",
        displayName = "AtmoDawnFog",
        aliases = {"atmo", "dawnfog"},
        category = ModuleCategory.VISUALS,
        description = "module.atmodawnfog.description"
)
public final class AtmoDawnFog extends Module {

    private static final float TIME_WINDOW = 0.1f;
    private static final float ALTITUDE_FADE = 32.0f;

    private final ModeValue mode =
            modeSetting("atmoDawnFogMode", "mode", "Dawn", "Dawn", "Dusk", "Custom");
    private final NumberValue<Float> density =
            num("atmoDawnFogDensity", "density", 0.35f, 0.05f, 0.8f);
    private final NumberValue<Float> scatterHeight =
            num("atmoDawnFogScatterHeight", "scatter_height", 76.0f, 60.0f, 120.0f);
    private final NumberValue<Float> softness =
            num("atmoDawnFogSoftness", "softness", 0.6f, 0.0f, 1.0f);
    private final RGBColorValue dawnColor =
            visibleWhen(colorNoAlpha("atmoDawnFogColor", "dawn_color", "#FFAD7A"), () -> "Custom".equals(mode.get()));

    private static int dawnArgb() {
        return 0xFFFFAD7A;
    }

    private static int duskArgb() {
        return 0xFF5C6BFF;
    }

    /** Сила эффекта 0..1: время суток, режим и высота камеры. */
    public float strength(double cameraY) {
        AtmoDawnFog module = Modules.get(AtmoDawnFog.class);
        if (module == null || !module.isEnabled()) return 0.0f;

        float timeOfDay = (WorldTweaks.getServerTimeOfDay() % 24000L) / 24000.0f;
        return strengthFor(timeOfDay, module.mode.get(), cameraY, module.scatterHeight.get());
    }

    /** Чистая математика силы эффекта — вынесена для тестов. */
    static float strengthFor(float timeOfDay, String mode, double cameraY, float scatterHeight) {
        float byMode = switch (mode) {
            case "Dusk" -> timeCloseness(timeOfDay, 0.5f);
            case "Custom" -> 1.0f;
            default -> timeCloseness(timeOfDay, 0.0f);
        };
        if (byMode <= 0.0f) return 0.0f;

        float altitude = 1.0f - (float) Math.max(0.0, cameraY - scatterHeight) / ALTITUDE_FADE;
        return byMode * Math.max(0.0f, Math.min(1.0f, altitude));
    }

    private static float timeCloseness(float timeOfDay, float target) {
        float delta = Math.abs(timeOfDay - target);
        delta = Math.min(delta, 1.0f - delta);
        return Math.max(0.0f, 1.0f - delta / TIME_WINDOW);
    }

    /**
     * Применяет туман к FogData. Вызывается из миксина до WorldTweaks-веток;
     * false — эффекта нет в этом кадре.
     */
    public static boolean applyFog(FogData data, Camera camera, ClientLevel world,
                                   float viewDistance, float tickDelta) {
        AtmoDawnFog module = Modules.get(AtmoDawnFog.class);
        if (module == null || !module.isEnabled() || data == null || world == null) return false;
        if (world.dimension() != Level.OVERWORLD) return false;
        if (camera.getFluidInCamera() != FogType.NONE) return false;

        WorldTweaks tweaks = Modules.get(WorldTweaks.class);
        if (tweaks != null && tweaks.isFogModifyEnabled()) return false;

        float strength = module.strength(camera.position().y);
        if (strength <= 0.0f) return false;

        float density = module.density.get();
        float far = Math.max(viewDistance * (1.0f - 0.75f * density * strength), viewDistance * 0.15f);
        float start = far * Math.max(0.05f, module.softness.get());

        data.environmentalStart = start;
        data.environmentalEnd = far;
        data.renderDistanceStart = start;
        data.renderDistanceEnd = far;
        data.skyEnd = far;
        data.cloudEnd = far;
        return true;
    }

    /** Подмешивает цвет зари в туман; true — цвет уже изменён. */
    public static boolean tintColor(org.joml.Vector4f color, ClientLevel world, double cameraY) {
        AtmoDawnFog module = Modules.get(AtmoDawnFog.class);
        if (module == null || !module.isEnabled() || color == null || world == null) return false;

        WorldTweaks tweaks = Modules.get(WorldTweaks.class);
        if (tweaks != null && tweaks.isFogModifyEnabled()) return false;

        float strength = module.strength(cameraY) * 0.8f;
        if (strength <= 0.0f) return false;

        int argb = switch (module.mode.get()) {
            case "Dusk" -> duskArgb();
            case "Custom" -> module.dawnColor.getArgb();
            default -> dawnArgb();
        };
        float r = ((argb >> 16) & 0xFF) / 255.0f;
        float g = ((argb >> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;

        color.set(
                color.x + (r - color.x) * strength,
                color.y + (g - color.y) * strength,
                color.z + (b - color.z) * strength,
                color.w
        );
        return true;
    }
}
