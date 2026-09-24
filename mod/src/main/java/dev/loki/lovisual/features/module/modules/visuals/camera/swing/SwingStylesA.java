package dev.loki.lovisual.features.module.modules.visuals.camera.swing;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

/**
 * Swing animation modes, group A: vanilla-like, block and directional styles.
 * Each mode is a pure transform over the pre-computed {@link SwingAnimator.Params}; registered
 * into the {@link SwingAnimator} dispatcher. See {@link SwingStylesB} / {@link SwingStylesC}.
 */
final class SwingStylesA {

    private SwingStylesA() { }

    static void register(Map<String, SwingAnimator.Style> m) {
        m.put("SmoothVanilla", SwingStylesA::smoothVanilla);
        m.put("SwipeBackDown", SwingStylesA::swipeBackDown);
        m.put("Swipe Back", SwingStylesA::swipeBack);
        m.put("New", SwingStylesA::neo);
        m.put("Block Down", SwingStylesA::blockDown);
        m.put("Back", SwingStylesA::back);
        m.put("7", SwingStylesA::seven);
        m.put("Block", SwingStylesA::block);
        m.put("Forward", SwingStylesA::forward);
        m.put("Glide", SwingStylesA::glide);
        m.put("Tap", SwingStylesA::tap);
        m.put("Blocking", SwingStylesA::blocking);
    }

    private static void smoothVanilla(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, -0.2f * anim, 0.0);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f * anim));
    }

    private static void swipeBackDown(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, 0.1, -0.1);
        ms.mulPose(Axis.YP.rotationDegrees(60.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(-60.0f));
        ms.translate(0.0, -0.3f * anim, 0.0);
    }

    private static void swipeBack(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float sin1 = p.sin1();
        float sin2 = p.sin2();
        float power = p.power();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, 0.1, -0.1);
        ms.mulPose(Axis.YP.rotationDegrees(60.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(-60.0f));
        ms.mulPose(Axis.YP.rotationDegrees(sin2 * sin1 * -5.0f));
        ms.mulPose(Axis.XP.rotationDegrees(-10.0f - power * 10.0f * anim));
        ms.mulPose(Axis.XP.rotationDegrees(-60.0f));
    }

    private static void neo(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float sin2 = p.sin2();
        float power = p.power();
        float progress = p.progress();
        ms.scale(1.0f + 0.1f * sin2, 1.0f, 1.0f);
        ms.translate(0.2, 0.1, -0.2);
        ms.mulPose(Axis.XP.rotationDegrees(60.0f));
        float swingAngle = -120.0f - power * 10.0f * anim;
        ms.mulPose(Axis.XP.rotationDegrees(swingAngle));
        ms.mulPose(Axis.YP.rotationDegrees(30.0f * sin2));
        ms.mulPose(Axis.ZP.rotationDegrees(15.0f * sin2));
        if (progress > 0.8f) {
            ms.mulPose(Axis.XP.rotationDegrees(10.0f * (progress - 0.8f) * 5.0f));
        }
    }

    private static void blockDown(PoseStack ms, SwingAnimator.Params p) {
        float sin2 = p.sin2();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, 0.1, -0.1);
        ms.translate(0.5, -0.1, 0.0);
        ms.mulPose(Axis.XP.rotationDegrees(sin2 * -9.0f));
        ms.translate(-0.5, 0.1, 0.0);
        ms.translate(0.5, -0.1, 0.0);
        ms.mulPose(Axis.YP.rotationDegrees(sin2 * -18.0f));
        ms.translate(-0.5, 0.1, 0.0);
        ms.mulPose(Axis.YP.rotationDegrees(50.0f));
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f));
        ms.mulPose(Axis.YP.rotationDegrees(50.0f));
    }

    private static void back(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float power = p.power();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.4, 0.1, -0.5);
        ms.mulPose(Axis.YP.rotationDegrees(90.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(-60.0f));
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f - power * 10.0f * anim));
    }

    private static void seven(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float power = p.power();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.mulPose(Axis.YP.rotationDegrees(15.0f * anim));
        ms.mulPose(Axis.ZP.rotationDegrees(-50.0f * anim));
        ms.mulPose(Axis.XP.rotationDegrees((-10.0f - power) * anim));
    }

    private static void block(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float power = p.power();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.4, 0.0, -0.5);
        ms.mulPose(Axis.YP.rotationDegrees(90.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(-30.0f));
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f - power * 10.0f * anim));
    }

    private static void forward(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(90.0f));
        ms.mulPose(Axis.XP.rotationDegrees(-1.35f));
        ms.mulPose(Axis.YP.rotationDegrees(90.0f));
        ms.mulPose(Axis.XP.rotationDegrees(50.0f));
        ms.mulPose(Axis.YP.rotationDegrees(-30.0f));
        ms.translate(0.2, 0.5, 1.3);
        ms.translate(0.9f * anim, 0.2f + 0.35f * anim, 0.3f * anim);
        ms.mulPose(Axis.YP.rotationDegrees(anim * 50.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(anim * -20.0f));
    }

    private static void glide(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(90.0f));
        ms.mulPose(Axis.XP.rotationDegrees(-1.35f));
        ms.mulPose(Axis.YP.rotationDegrees(90.0f));
        ms.mulPose(Axis.XP.rotationDegrees(50.0f));
        ms.mulPose(Axis.YP.rotationDegrees(-30.0f));
        ms.translate(0.2, 0.5, 1.3);
        ms.translate(0.0, 0.7f * anim + 0.2, -0.7f * anim);
        ms.mulPose(Axis.XP.rotationDegrees(anim * -70.0f));
    }

    private static void tap(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.mulPose(Axis.XP.rotationDegrees(-60.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(60.0f));
        ms.mulPose(Axis.XP.rotationDegrees(-0.9f));
        ms.mulPose(Axis.YP.rotationDegrees(60.0f));
        ms.translate(0.6, 0.0, 1.0);
        ms.translate(0.4f * anim, 0.0, 0.0);
    }

    private static void blocking(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(90.0f));
        ms.mulPose(Axis.YP.rotationDegrees(40.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(-20.0f));
        ms.translate(0.0, 0.3, 0.9);
        ms.translate(0.0, 0.6f * anim, -0.6f * anim);
        ms.mulPose(Axis.XP.rotationDegrees(anim * -45.0f));
    }
}
