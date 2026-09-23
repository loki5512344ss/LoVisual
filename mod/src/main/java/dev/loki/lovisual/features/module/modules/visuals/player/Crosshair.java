/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.module.modules.visuals.player;

import dev.loki.lovisual.config.common.CommonSettingSchemas;
import dev.loki.lovisual.config.values.color.RGBAColorValue;
import dev.loki.lovisual.config.values.mode.EnumValue;
import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.features.gui.hud.draggable.impl.combat.Itemizer;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleCategory;
import dev.loki.lovisual.features.module.core.ModuleInfo;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.render.engine.animation.AnimatedRenderColors;
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@ModuleInfo(id = "crosshair", displayName = "Crosshair", category = ModuleCategory.VISUALS)
public class Crosshair extends Module {
    final EnumValue<CrosshairType> crosshairType = enumSetting("crosshairType", "type", CrosshairType.CLASSIC);
    final EnumValue<OrbizMotionMode> orbizMotion = visibleWhen(enumSetting("crosshairOrbizMotion", "orbiz_motion", OrbizMotionMode.DYNAMIC), this::isOrbiz);
    final BooleanValue showOutline = visibleWhen(bool("crosshairOutline", "outline", true), this::isClassicCrosshair);
    final BooleanValue dynamicGap = visibleWhen(bool("crosshairDynamicGap", "dynamic_gap", true), this::isClassicCrosshair);
    final BooleanValue highlightTarget = bool("crosshairHighlightTarget", "highlight_target", true);
    final NumberValue<Double> thickness = visibleWhen(num("crosshairThickness", "thickness", 0.5, 0.5, 1.0), this::isClassicCrosshair);
    final NumberValue<Double> length = visibleWhen(num("crosshairLength", "length", 3.0, 1.0, 10.0), this::isClassicCrosshair);
    final NumberValue<Double> baseGap = visibleWhen(num("crosshairBaseGap", "base_gap", 4.0, 0.0, 10.0), this::isClassicCrosshair);
    final NumberValue<Double> maxGapIncrease = visibleWhen(num("crosshairMaxGapIncrease", "max_gap_increase", 7.0, 0.0, 15.0), () -> isClassicCrosshair() && dynamicGap.get());
    final NumberValue<Double> orbizRadius = visibleWhen(num("crosshairOrbizRadius", "orbiz_radius", 5.0, 2.0, 5.0), this::isOrbiz);
    final NumberValue<Double> orbizThickness = visibleWhen(num("crosshairOrbizThickness", "orbiz_thickness", 1.35, 0.5, 5.0), this::isOrbiz);
    final NumberValue<Double> orbizSoftness = visibleWhen(num("crosshairOrbizSoftness", "orbiz_softness", 0.35, 0.0, 2.0), this::isOrbiz);
    final NumberValue<Double> orbizGlow = visibleWhen(num("crosshairOrbizGlow", "orbiz_glow", 5.5, 0.0, 20.0), this::isOrbiz);
    final NumberValue<Double> orbizGlowStrength = visibleWhen(num("crosshairOrbizGlowStrength", "orbiz_glow_strength", 0.72, 0.0, 2.0), this::isOrbiz);
    final NumberValue<Double> orbizCooldownMultiplier = visibleWhen(num("crosshairOrbizCooldownMultiplier", "orbiz_cooldown_multiplier", 1.0, 0.1, 1.5), this::isOrbiz);
    final NumberValue<Double> orbizTrackAlpha = visibleWhen(num("crosshairOrbizTrackAlpha", "orbiz_track_alpha", 0.24, 0.0, 1.0), this::isOrbiz);
    final NumberValue<Double> orbizBodyAlpha = visibleWhen(num("crosshairOrbizBodyAlpha", "orbiz_body_alpha", 0.92, 0.0, 1.0), this::isOrbiz);
    final NumberValue<Double> orbizHeadBoost = visibleWhen(num("crosshairOrbizHeadBoost", "orbiz_head_boost", 0.30, 0.0, 1.0), this::isOrbiz);
    final NumberValue<Double> orbizAngularCycles = visibleWhen(num("crosshairOrbizAngularCycles", "orbiz_angular_cycles", 1.0, 0.25, 4.0), this::isOrbiz);
    final EnumValue<AnimatedRenderColors.Mode> orbizColorMode = visibleWhen(common(enumSetting("crosshairOrbizColorMode", "orbiz_color_mode", AnimatedRenderColors.Mode.RAINBOW, AnimatedRenderColors.Mode.STATIC, AnimatedRenderColors.Mode.RAINBOW, AnimatedRenderColors.Mode.LIGHT_RAINBOW, AnimatedRenderColors.Mode.SKY, AnimatedRenderColors.Mode.FADE, AnimatedRenderColors.Mode.DOUBLE_COLOR, AnimatedRenderColors.Mode.ANALOGOUS, AnimatedRenderColors.Mode.THEME), CommonSettingSchemas.RENDER_COLOR_MODE.commonI18nKey()), this::isOrbiz);
    final NumberValue<Integer> orbizColorSpeed = visibleWhen(common(num("crosshairOrbizColorSpeed", "orbiz_color_speed", 18, 2, 54), CommonSettingSchemas.RENDER_COLOR_SPEED.commonI18nKey()), this::isOrbiz);
    final RGBAColorValue orbizColor = visibleWhen(common(color("crosshairOrbizColor", "orbiz_color", "#EEFFFFFF"), CommonSettingSchemas.RENDER_PRIMARY_COLOR.commonI18nKey()), this::isOrbiz);
    final RGBAColorValue orbizColor2 = visibleWhen(common(color("crosshairOrbizColor2", "orbiz_color2", "#EE55FFFF"), CommonSettingSchemas.RENDER_SECONDARY_COLOR.commonI18nKey()), () -> isOrbiz() && AnimatedRenderColors.usesSecondary(orbizColorMode.get()));
    final NumberValue<Double> orbizLookMultiplier = visibleWhen(num("crosshairOrbizLookMultiplier", "orbiz_look_multiplier", 0.45, 0.0, 2.5), () -> isOrbiz() && isOrbizMotionDynamic());
    final NumberValue<Double> orbizMoveMultiplier = visibleWhen(num("crosshairOrbizMoveMultiplier", "orbiz_move_multiplier", 2.0, 0.0, 10.0), () -> isOrbiz() && isOrbizMotionDynamic());
    final NumberValue<Double> orbizMotionLimit = visibleWhen(num("crosshairOrbizMotionLimit", "orbiz_motion_limit", 8.0, 0.0, 24.0), () -> isOrbiz() && isOrbizMotionDynamic());
    final NumberValue<Double> orbizMotionSmoothing = visibleWhen(num("crosshairOrbizMotionSmoothing", "orbiz_motion_smoothing", 12.0, 1.0, 30.0), () -> isOrbiz() && isOrbizMotionDynamic());

    final CrosshairCombatHelper combat = new CrosshairCombatHelper(this);
    final CrosshairColorHelper color = new CrosshairColorHelper(this);
    final CrosshairOrbizHelper orbiz = new CrosshairOrbizHelper(this);
    final CrosshairRenderHelper render = new CrosshairRenderHelper(this);

    @Override public HudPhase getHudPhase() { return HudPhase.LAST; }

    public boolean shouldHideVanilla() {
        Minecraft mc = Minecraft.getInstance();
        return isEnabled() && mc.player != null && mc.level != null && mc.options.getCameraType().isFirstPerson();
    }

    @Override public void onEnable() { combat.resetSwapDetector(); orbiz.resetOrbizState(); color.reset(); render.reset(); }
    @Override public void onDisable() { combat.resetSwapDetector(); orbiz.resetOrbizState(); Itemizer.hideElytraSwap(); }

    @Override
    public void onRenderHudEngine(Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (!isEnabled() || mc.player == null || mc.level == null) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        float scale = ViewportContext.getScaleFactor();
        float centerX = mc.getWindow().getWidth() * 0.5f;
        float centerY = mc.getWindow().getHeight() * 0.5f;
        float thicknessPx = (float) (thickness.get() * scale);
        float lengthPx = (float) (length.get() * scale);
        float outlinePx = render.outlineExpand(scale);
        float cooldown = render.computeCooldown();
        float targetGap = (float) (baseGap.get() + maxGapIncrease.get() * cooldown);
        float gapPx = render.updateGap(targetGap, scale);
        combat.updateSwapDetector(mc.player);
        if (crosshairType.get() == CrosshairType.ORBIZ) { orbiz.renderOrbiz(renderer, centerX, centerY, scale); return; }
        boolean outline = showOutline.get();
        if (crosshairType.get() == CrosshairType.DOT) {
            int c = color.smoothCrosshairColor(color.resolveCrosshairTargetColor());
            render.drawSegment(renderer, centerX - thicknessPx, centerY - thicknessPx, thicknessPx * 2.0f, thicknessPx * 2.0f, c, outline, outlinePx);
            return;
        }
        if (crosshairType.get() == CrosshairType.PINWHEEL) {
            int c = color.smoothCrosshairColor(color.resolveCrosshairTargetColor());
            render.renderPinwheel(renderer, centerX, centerY, thicknessPx, lengthPx, c);
            return;
        }
        if (combat.shouldShowRiptideBlocked(mc.player) || combat.shouldShowCooldownCross(mc.player)) {
            int c = color.smoothCrosshairColor(render.riptideColor());
            render.renderRiptideBlockedCross(renderer, centerX, centerY, thicknessPx, lengthPx, outline, outlinePx, c);
            return;
        }
        int c = color.smoothCrosshairColor(color.resolveCrosshairTargetColor());
        if (crosshairType.get() == CrosshairType.T_CLASSIC) {
            render.drawSegment(renderer, centerX - lengthPx, centerY - thicknessPx * 0.5f, lengthPx * 2.0f, thicknessPx, c, outline, outlinePx);
            render.drawSegment(renderer, centerX - thicknessPx * 0.5f, centerY, thicknessPx, lengthPx + gapPx, c, outline, outlinePx);
            return;
        }
        render.drawSegment(renderer, centerX - thicknessPx * 0.5f, centerY - gapPx - lengthPx, thicknessPx, lengthPx, c, outline, outlinePx);
        render.drawSegment(renderer, centerX - thicknessPx * 0.5f, centerY + gapPx, thicknessPx, lengthPx, c, outline, outlinePx);
        render.drawSegment(renderer, centerX - gapPx - lengthPx, centerY - thicknessPx * 0.5f, lengthPx, thicknessPx, c, outline, outlinePx);
        render.drawSegment(renderer, centerX + gapPx, centerY - thicknessPx * 0.5f, lengthPx, thicknessPx, c, outline, outlinePx);
    }

    boolean isOrbiz() { return crosshairType.get() == CrosshairType.ORBIZ; }
    boolean isClassicCrosshair() { return crosshairType.get() != CrosshairType.ORBIZ; }
    boolean isOrbizMotionDynamic() { return orbizMotion.get() == OrbizMotionMode.DYNAMIC; }

    enum CrosshairType implements EnumValue.AliasProvider {
        CLASSIC, T_CLASSIC, ORBIZ, PINWHEEL, DOT;
        @Override public List<String> aliases() {
            return switch (this) {
                case CLASSIC -> List.of("Classic");
                case T_CLASSIC -> List.of("T-Classic");
                case ORBIZ -> List.of("Orbiz");
                case PINWHEEL -> List.of("Pinwheel");
                case DOT -> List.of("Dot");
            };
        }
    }

    enum OrbizMotionMode implements EnumValue.AliasProvider {
        STATIC, DYNAMIC;
        @Override public List<String> aliases() {
            return switch (this) {
                case STATIC -> List.of("Static", "Off", "Disabled", "false");
                case DYNAMIC -> List.of("Dynamic", "On", "Enabled", "true");
            };
        }
    }
}
