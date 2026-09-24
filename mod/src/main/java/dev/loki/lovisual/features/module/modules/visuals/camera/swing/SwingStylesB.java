package dev.loki.lovisual.features.module.modules.visuals.camera.swing;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.loki.lovisual.util.time.GameClock;

/**
 * Swing animation modes, group B: touch, spin, circular and flip styles.
 * Each mode is a pure transform over the pre-computed {@link SwingAnimator.Params}; registered
 * into the {@link SwingAnimator} dispatcher. See {@link SwingStylesA} / {@link SwingStylesC}.
 */
final class SwingStylesB {

    private SwingStylesB() { }

    static void register(Map<String, SwingAnimator.Style> m) {
        m.put("Touch", SwingStylesB::touch);
        m.put("Slant", SwingStylesB::slant);
        m.put("Spin", SwingStylesB::spin);
        m.put("Helicopter", SwingStylesB::helicopter);
        m.put("Stab", SwingStylesB::stab);
        m.put("Circle", SwingStylesB::circle);
        m.put("Wave", SwingStylesB::wave);
        m.put("360 Swing", SwingStylesB::swing360);
        m.put("Vanilla", SwingStylesB::vanilla);
        m.put("Flip", SwingStylesB::flip);
        m.put("Side", SwingStylesB::side);
    }

    private static void touch(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f));
        ms.mulPose(Axis.ZP.rotationDegrees(90.0f));
        ms.mulPose(Axis.XP.rotationDegrees(-1.35f));
        ms.mulPose(Axis.YP.rotationDegrees(90.0f));
        ms.translate(-0.3, 0.0, 1.3);
        ms.translate(0.0, 0.0, 0.2f * anim);
        ms.mulPose(Axis.XP.rotationDegrees(anim * 15.0f));
    }

    private static void slant(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        float rotate = 35.0f;
        ms.translate(0.0, 0.0, -0.3f * anim);
        ms.mulPose(Axis.XP.rotationDegrees(anim * -rotate));
        ms.mulPose(Axis.ZP.rotationDegrees(anim * rotate));
    }

    private static void spin(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float power = p.power();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, 0.1, -0.2);
        ms.mulPose(Axis.YP.rotationDegrees(anim * 360.0f * power));
        ms.mulPose(Axis.XP.rotationDegrees(45.0f));
    }

    private static void helicopter(PoseStack ms, SwingAnimator.Params p) {
        float power = p.power();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, -0.2, 0.0);
        float rotation = (GameClock.millis() % 2000L) / 2000.0f * 360.0f * power;
        ms.mulPose(Axis.ZP.rotationDegrees(rotation));
        ms.translate(0.0, 0.3, 0.0);
    }

    private static void stab(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float power = p.power();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, 0.0, -0.5f * anim * power);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f));
    }

    private static void circle(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float power = p.power();
        float progress = p.progress();
        ms.scale(1.0f, 1.0f, 1.0f);
        float circleProgress = progress * 2.0f * (float) Math.PI;
        ms.translate(Math.sin(circleProgress) * 0.5f * power, Math.cos(circleProgress) * 0.3 * power, 0.0);
        ms.mulPose(Axis.ZP.rotationDegrees(anim * 180.0f));
    }

    private static void wave(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float power = p.power();
        float progress = p.progress();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, Math.sin(progress * Math.PI * 2.0f) * 0.3 * power, 0.0);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f + anim * 45.0f * power));
    }

    private static void swing360(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float power = p.power();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, 0.1, -0.1);
        ms.mulPose(Axis.XP.rotationDegrees(anim * 360.0f * power));
    }

    private static void vanilla(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f * anim));
    }

    private static void flip(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float power = p.power();
        ms.scale(1.0f, 1.0f, 1.0f);
        float angle = anim * 180.0f * power;
        ms.mulPose(Axis.ZP.rotationDegrees(angle));
        ms.translate(0.0, 0.0, -0.3f * anim);
    }

    private static void side(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.4f * anim, 0.0, -0.2f * anim);
        ms.mulPose(Axis.YP.rotationDegrees(-60.0f * anim));
    }
}
