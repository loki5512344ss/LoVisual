package dev.loki.lovisual.features.module.modules.visuals.scene.visual;

import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.helpers.material.SodiumMaterialFlags;

final class ReimaginedVegetationHelper {
    private final NumberValue<Float> rootedHorizontal;
    private final NumberValue<Float> rootedVertical;
    private final NumberValue<Float> freeHorizontal;
    private final NumberValue<Float> freeVertical;
    private final NumberValue<Float> speed;
    private boolean initialized;
    private boolean lastActive;
    private int lastPacked;

    ReimaginedVegetationHelper(
            NumberValue<Float> rootedHorizontal,
            NumberValue<Float> rootedVertical,
            NumberValue<Float> freeHorizontal,
            NumberValue<Float> freeVertical,
            NumberValue<Float> speed) {
        this.rootedHorizontal = rootedHorizontal;
        this.rootedVertical = rootedVertical;
        this.freeHorizontal = freeHorizontal;
        this.freeVertical = freeVertical;
        this.speed = speed;
    }

    void onTick(boolean moduleEnabled, boolean effectSelected) {
        refresh(moduleEnabled && effectSelected);
    }

    void onEnable(boolean effectSelected) {
        refresh(effectSelected);
    }

    void onDisable() {
        refresh(false);
    }

    void refresh(boolean active) {
        int packed = active ? pack() : 0;
        if (!initialized) {
            initialized = true;
            lastActive = false;
            lastPacked = 0;
        }
        if (lastActive == active && lastPacked == packed) {
            return;
        }
        lastActive = active;
        lastPacked = packed;
        LoVisualRenderSystem.sodium().reloadWorldRenderer();
    }

    int pack() {
        return pack(rootedHorizontal.get(), rootedVertical.get(), freeHorizontal.get(), freeVertical.get(), speed.get());
    }

    int pack(float rootedH, float rootedV, float freeH, float freeV, float sp) {
        return (encode(rootedH) << SodiumMaterialFlags.WAVE_ROOTED_HORIZONTAL_SHIFT)
                | (encode(rootedV) << SodiumMaterialFlags.WAVE_ROOTED_VERTICAL_SHIFT)
                | (encode(freeH) << SodiumMaterialFlags.WAVE_FREE_HORIZONTAL_SHIFT)
                | (encode(freeV) << SodiumMaterialFlags.WAVE_FREE_VERTICAL_SHIFT)
                | (encode(sp) << SodiumMaterialFlags.WAVE_SPEED_SHIFT);
    }

    private int encode(float value) {
        float clamped = Math.max(0.0f, Math.min(SodiumMaterialFlags.WAVE_SETTING_MAX, value));
        return Math.round((clamped / SodiumMaterialFlags.WAVE_SETTING_MAX) * SodiumMaterialFlags.WAVE_SETTING_MASK);
    }
}
