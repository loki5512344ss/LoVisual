/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.iris.state;

import com.mojang.blaze3d.vertex.PoseStack;

public enum IrisHandMaskState {
    ;

    public static void renderBothPhases(Runnable renderer) {
        renderBothPhases(null, renderer);
    }

    public static void renderBothPhases(PoseStack matrices, Runnable renderer) {
        if (matrices != null) {
            matrices.pushPose();
        }
        try {
            renderer.run();
        } finally {
            if (matrices != null) {
                matrices.popPose();
            }
        }
    }
}
