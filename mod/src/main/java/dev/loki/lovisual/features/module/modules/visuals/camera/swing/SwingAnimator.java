package dev.loki.lovisual.features.module.modules.visuals.camera.swing;

import java.util.HashMap;
import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Baked per-mode hand swing animation transforms for {@code ViewModel}.
 * Pure geometry: given the animation progress scalars and the user power it applies the
 * translate / rotate / scale matrix for the selected swing mode. The concrete per-mode
 * transforms live in {@link SwingStylesA} and {@link SwingStylesB}; this class only dispatches
 * and provides the fallback used for unknown modes. Extracted so the module stays under the cap.
 */
public final class SwingAnimator {

    private SwingAnimator() { }

    /** A single swing animation mode transform. */
    @FunctionalInterface
    public interface Style {
        void apply(PoseStack ms, Params p);
    }

    /** Pre-computed animation scalars shared by every swing mode. */
    public record Params(float progress, float anim, float sin1, float sin2, float power) { }

    private static final Map<String, Style> REGISTRY = new HashMap<>();

    static {
        SwingStylesA.register(REGISTRY);
        SwingStylesB.register(REGISTRY);
        SwingStylesC.register(REGISTRY);
    }

    /** Apply the transform stack for the given swing {@code mode} onto {@code ms}. */
    public static void apply(String mode, PoseStack ms, Params p) {
        REGISTRY.getOrDefault(mode, SwingAnimator::defaultStyle).apply(ms, p);
    }

    private static void defaultStyle(PoseStack ms, Params p) {
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f * p.anim() * p.power() * 0.35f));
    }
}
