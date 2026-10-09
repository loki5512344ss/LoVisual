package dev.loki.lovisual.features.module.modules.visuals.camera;

import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;

@ModuleInfo(
        id = "cameraclip",
        displayName = "CameraClip",
        category = ModuleCategory.VISUALS
)
public final class CameraClip extends Module {

    private final NumberValue<Double> distanceMultiplier =
            num(
                    "cameraClipDistanceMultiplier",
                    "distance_multiplier",
                    1.0,
                    0.1,
                    5.0
            );

    private final NumberValue<Double> backOffset =
            num(
                    "cameraClipBackOffset",
                    "back_offset",
                    0.0,
                    -10.0,
                    10.0
            );

    private final NumberValue<Double> rightOffset =
            num(
                    "cameraClipRightOffset",
                    "right_offset",
                    0.0,
                    -10.0,
                    10.0
            );

    private final NumberValue<Double> upOffset =
            num(
                    "cameraClipUpOffset",
                    "up_offset",
                    0.0,
                    -10.0,
                    10.0
            );

    public double getDistanceMultiplier() {
        return isEnabled() ? distanceMultiplier.get() : 1.0;
    }

    public double getBackOffset() {
        return isEnabled() ? backOffset.get() : 0.0;
    }

    public double getRightOffset() {
        return isEnabled() ? rightOffset.get() : 0.0;
    }

    public double getUpOffset() {
        return isEnabled() ? upOffset.get() : 0.0;
    }

    public float distanceMultiplier() {
        return (float) getDistanceMultiplier();
    }

    public float backOffset() {
        return (float) getBackOffset();
    }

    public float rightOffset() {
        return (float) getRightOffset();
    }

    public float upOffset() {
        return (float) getUpOffset();
    }
}
