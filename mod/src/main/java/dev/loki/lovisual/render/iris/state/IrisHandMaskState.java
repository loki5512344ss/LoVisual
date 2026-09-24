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
