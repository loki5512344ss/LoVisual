package dev.loki.lovisual.features.module.modules.visuals.camera.swing;

import java.util.Map;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
import dev.loki.lovisual.util.time.GameClock;

/**
 * Swing animation modes, group C: strike, chop and smooth styles.
 * Each mode is a pure transform over the pre-computed {@link SwingAnimator.Params}; registered
 * into the {@link SwingAnimator} dispatcher. See {@link SwingStylesA} / {@link SwingStylesB}.
 */
final class SwingStylesC {

    private SwingStylesC() { }

    static void register(Map<String, SwingAnimator.Style> m) {
        m.put("Overhead", SwingStylesC::overhead);
        m.put("Hammer", SwingStylesC::hammer);
        m.put("Chop", SwingStylesC::chop);
        m.put("Arc", SwingStylesC::arc);
        m.put("Double", SwingStylesC::dbl);
        m.put("Shake", SwingStylesC::shake);
        m.put("Shove", SwingStylesC::shove);
        m.put("Slash", SwingStylesC::slash);
        m.put("Thrust", SwingStylesC::thrust);
        m.put("Smooth Down", SwingStylesC::smoothDown);
        m.put("Smooth", SwingStylesC::smooth);
    }

    private static void overhead(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, 0.5, 0.0);
        ms.mulPose(Axis.XP.rotationDegrees(-150.0f * anim));
    }

    private static void hammer(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.3, 0.3, -0.3);
        ms.mulPose(Axis.ZP.rotationDegrees(30.0f));
        ms.mulPose(Axis.XP.rotationDegrees(-120.0f * anim));
    }

    private static void chop(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, -0.2f * anim, -0.3f * anim);
        ms.mulPose(Axis.XP.rotationDegrees(-90.0f * anim));
    }

    private static void arc(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        float a = anim * 90.0f;
        ms.mulPose(Axis.YP.rotationDegrees(a));
        ms.mulPose(Axis.XP.rotationDegrees(-a));
    }

    private static void dbl(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        float d = anim * 720.0f;
        ms.mulPose(Axis.XP.rotationDegrees(d));
    }

    private static void shake(PoseStack ms, SwingAnimator.Params p) {
        ms.scale(1.0f, 1.0f, 1.0f);
        float s = (float) Math.sin((GameClock.millis() % 200L) / 200.0f * Math.PI * 2.0f) * 5.0f;
        ms.mulPose(Axis.ZP.rotationDegrees(s));
    }

    private static void shove(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, 0.0, -0.6f * anim);
    }

    private static void slash(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.mulPose(Axis.YP.rotationDegrees(90.0f));
        ms.mulPose(Axis.XP.rotationDegrees(-120.0f * anim));
    }

    private static void thrust(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, 0.0, -1.0f * anim);
    }

    private static void smoothDown(PoseStack ms, SwingAnimator.Params p) {
        float anim = p.anim();
        float power = p.power();
        float progress = p.progress();
        ms.scale(1.0f, 1.0f, 1.0f);
        ms.translate(0.0, -0.1f * anim, 0.0);
        float down = Mth.sin(progress * (float) Math.PI) * -65.0f * power;
        ms.mulPose(Axis.XP.rotationDegrees(down));
    }

    private static void smooth(PoseStack ms, SwingAnimator.Params p) {
        float power = p.power();
        float progress = p.progress();
        ms.scale(1.0f, 1.0f, 1.0f);
        float s = Mth.sqrt(progress);
        float back = Mth.sin(s * (float) Math.PI) * 25.0f * power;
        float down = Mth.sin(progress * (float) Math.PI) * -55.0f * power;
        ms.mulPose(Axis.XP.rotationDegrees(back + down));
    }
}
