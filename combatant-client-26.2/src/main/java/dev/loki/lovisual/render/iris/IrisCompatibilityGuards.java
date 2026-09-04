/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.iris;

import dev.loki.lovisual.features.module.Modules;
import dev.loki.lovisual.features.module.modules.visuals.MotionBlur;
import dev.loki.lovisual.Lifecycle;

public enum IrisCompatibilityGuards {
    ;

    public static boolean suppressShaderpackMotionBlur() {
        if (!Lifecycle.canRunShaderBridge()) return false;
        MotionBlur module = Modules.get(MotionBlur.class);
        return IrisRuntime.isShaderpackRendererActive()
                && IrisRuntime.supports(IrisCompatibilityFeature.MOTION_BLUR_POLICY)
                && module != null
                && module.isActive();
    }

    public static boolean suppressLoVisualTerrainShaderOverrides() {
        if (!Lifecycle.canRunShaderBridge()) return false;
        return IrisRuntime.isModLoaded();
    }

    public static boolean suppressIrisHandRendering() {
        // Do not cancel Iris' own hand renderer. Iris redirects vanilla hand submit to a no-op
        // while a shaderpack is active, so suppressing HandRenderer removes the visible hand entirely.
        // Chams builds its mask as an extra pass and must not own the visible hand path.
        return false;
    }

    public static boolean deferIrisFinalizationForSecondHandScene() {
        if (!Lifecycle.canRunShaderBridge()) return false;
        return IrisRuntime.isShaderpackRendererActive();
    }
}
