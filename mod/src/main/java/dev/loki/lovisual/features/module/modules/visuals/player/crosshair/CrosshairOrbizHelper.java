package dev.loki.lovisual.features.module.modules.visuals.player;

import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.math.ColorMath;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

final class CrosshairOrbizHelper {

    private static final float ORBIZ_PROGRESS_SMOOTH_SPEED = 22.0f;
    private static final float COOLDOWN_LEAD = 0.06f;
    private static final int RIPTIDE_BLOCK_COLOR = 0xFFFF3B3B;

    private final Crosshair host;
    private float animatedOrbizProgress = 1.0f;
    private float orbizOffsetX;
    private float orbizOffsetY;
    private float lastOrbizYaw;
    private float lastOrbizPitch;
    private boolean orbizMotionInitialized;

    CrosshairOrbizHelper(Crosshair host) {
        this.host = host;
    }

    void resetOrbizState() {
        animatedOrbizProgress = 1.0f;
        orbizOffsetX = 0.0f;
        orbizOffsetY = 0.0f;
        lastOrbizYaw = 0.0f;
        lastOrbizPitch = 0.0f;
        orbizMotionInitialized = false;
    }

    void renderOrbiz(Renderer2D renderer, float centerX, float centerY, float scale) {
        Minecraft mc = Minecraft.getInstance();
        boolean danger = host.combat.shouldShowRiptideBlocked(mc.player) || host.combat.shouldShowCooldownCross(mc.player);
        float progress = danger ? 1.0f : resolveOrbizProgress();
        float targetProgress = Mth.clamp(progress * host.orbizCooldownMultiplier.get().floatValue(), 0.0f, 1.0f);
        if (targetProgress < animatedOrbizProgress) {
            animatedOrbizProgress = targetProgress;
        } else {
            float t = AnimationUtility.clamp01(AnimationUtility.deltaTime() * ORBIZ_PROGRESS_SMOOTH_SPEED);
            animatedOrbizProgress = AnimationUtility.lerp(animatedOrbizProgress, targetProgress, t);
        }
        updateOrbizMotion();
        AnimatedRenderColors.Mode mode = danger ? AnimatedRenderColors.Mode.STATIC : host.orbizColorMode.get();
        int primary = danger ? colorWithConfigAlpha(RIPTIDE_BLOCK_COLOR, host.orbizColor.getArgb()) : resolveOrbizPrimary(mode);
        int secondary = danger ? primary : AnimatedRenderColors.angularSecondaryColor(mode, primary, host.orbizColor2.getArgb());
        primary = AnimatedRenderColors.angularPrimaryColor(mode, primary);
        float radiusPx = (float) (host.orbizRadius.get() * scale);
        float thicknessPx = (float) (host.orbizThickness.get() * scale);
        float softnessPx = (float) (host.orbizSoftness.get() * scale);
        float glowPx = (float) (host.orbizGlow.get() * scale);
        float sweep = animatedOrbizProgress * 360.0f;
        float trackGlow = glowPx > 0.0f ? 1.0f : 0.0f;
        renderer.orbizRing(
                centerX + orbizOffsetX * scale,
                centerY + orbizOffsetY * scale,
                radiusPx, thicknessPx, 0.0f, sweep, softnessPx, glowPx,
                host.orbizGlowStrength.get().floatValue(), primary, secondary,
                AnimatedRenderColors.shaderMode(mode),
                AnimatedRenderColors.angularOffset01(mode, host.orbizColorSpeed.get()),
                host.orbizAngularCycles.get().floatValue(),
                host.orbizTrackAlpha.get().floatValue(),
                host.orbizBodyAlpha.get().floatValue(),
                host.orbizHeadBoost.get().floatValue(),
                trackGlow, true);
    }

    private float resolveOrbizProgress() {
        Minecraft mc = Minecraft.getInstance();
        float charge = host.combat.getChargeProgress(mc.player);
        if (charge >= 0.0f) return Mth.clamp(charge, 0.0f, 1.0f);
        return Mth.clamp(host.combat.getAttackCooldownProgress() + COOLDOWN_LEAD, 0.0f, 1.0f);
    }

    private void updateOrbizMotion() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            orbizOffsetX = AnimationUtility.fast(orbizOffsetX, 0.0f, 10.0f);
            orbizOffsetY = AnimationUtility.fast(orbizOffsetY, 0.0f, 10.0f);
            orbizMotionInitialized = false;
            return;
        }
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        if (!orbizMotionInitialized) {
            lastOrbizYaw = yaw;
            lastOrbizPitch = pitch;
            orbizMotionInitialized = true;
        }
        float targetX = 0.0f;
        float targetY = 0.0f;
        if (host.isOrbizMotionDynamic()) {
            float look = host.orbizLookMultiplier.get().floatValue();
            float move = host.orbizMoveMultiplier.get().floatValue();
            targetX = Mth.wrapDegrees(lastOrbizYaw - yaw) * look + mc.player.xxa * move;
            targetY = (lastOrbizPitch - pitch) * look + mc.player.zza * move;
            float limit = host.orbizMotionLimit.get().floatValue();
            targetX = Mth.clamp(targetX, -limit, limit);
            targetY = Mth.clamp(targetY, -limit, limit);
        }
        float t = AnimationUtility.clamp01(AnimationUtility.deltaTime() * host.orbizMotionSmoothing.get().floatValue());
        orbizOffsetX = AnimationUtility.lerp(orbizOffsetX, targetX, t);
        orbizOffsetY = AnimationUtility.lerp(orbizOffsetY, targetY, t);
        lastOrbizYaw = yaw;
        lastOrbizPitch = pitch;
    }

    private int resolveOrbizPrimary(AnimatedRenderColors.Mode mode) {
        int configured = host.orbizColor.getArgb();
        if (mode == AnimatedRenderColors.Mode.STATIC && host.highlightTarget.get()) {
            return colorWithConfigAlpha(host.color.smoothCrosshairColor(host.color.resolveCrosshairTargetColor()), configured);
        }
        return configured;
    }

    private int colorWithConfigAlpha(int rgbSource, int alphaSource) {
        return ColorMath.colorWithAlpha(rgbSource, (alphaSource >>> 24) & 0xFF);
    }
}
