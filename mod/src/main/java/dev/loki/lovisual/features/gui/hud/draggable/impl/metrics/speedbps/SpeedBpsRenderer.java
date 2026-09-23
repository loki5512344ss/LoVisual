/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.metrics;

import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.features.gui.hud.script.ScriptedCompactHudStatRenderer;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

final class SpeedBpsRenderer {

    private SpeedBpsRenderer() {
    }

    static void render(SpeedBps host,
                       Renderer2D renderer,
                       TextRenderer textRenderer,
                       GuiGraphicsExtractor ctx,
                       float tickDelta,
                       int screenW,
                       int screenH) {
        Minecraft mc = Minecraft.getInstance();
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        if (mc == null && !preview) {
            host.setHudSize(0f, 0f);
            host.scriptModel.setVisible(false);
            return;
        }
        if (!preview && !host.isEnabled()) {
            host.setHudSize(0f, 0f);
            host.scriptModel.setVisible(false);
            return;
        }

        float currentBps = preview ? 0f : SpeedBpsSampler.currentBps();
        SpeedBpsSampler.updateSamples(host, currentBps);

        boolean showBps = host.useBps.get();
        boolean useAverage = host.average.get();
        float baseBps = useAverage ? SpeedBpsSampler.averageBps(host, currentBps) : currentBps;
        host.displayedSpeed = Mth.lerp(0.35f, host.displayedSpeed, baseBps);

        float valueSpeed = showBps ? host.displayedSpeed : host.displayedSpeed * SpeedBps.KMH_MULT;
        String value = SpeedBpsSampler.getSpeedNumber(host, valueSpeed);
        String unit = showBps ? "b/s" : "km/h";
        SpeedBpsPalette.update(host);

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer valueRenderer = Fonts.renderer("Onest", FontInfo.Type.Regular, fallback);
        TextRenderer metaRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, valueRenderer);

        float drawScale = HudScale.scale(screenW, screenH)
                * (host.hud.getFontSize() / 18f)
                * SpeedBps.SCALE_MULT
                * host.scale.get().floatValue();

        valueRenderer.begin(drawScale, true, false);
        float valueW = (float) valueRenderer.getWidth(value, false);
        float valueH = (float) valueRenderer.getHeight(false);
        valueRenderer.end();

        metaRenderer.begin(drawScale, true, false);
        float unitW = (float) metaRenderer.getWidth(unit, false);
        float unitH = (float) metaRenderer.getHeight(false);
        metaRenderer.end();

        float boxH = SpeedBps.BOX_HEIGHT * drawScale;
        float iconX = host.hudX() + SpeedBps.ICON_X * drawScale;
        float iconSize = SpeedBps.ICON_SIZE * drawScale;
        float unitGap = 2f * drawScale;
        host.displayValueWidth = smoothWidth(host.displayValueWidth, valueW);
        float valueX = host.hudX() + SpeedBps.TEXT_LEFT_X * drawScale;
        float boxW = (valueX - host.hudX()) + host.displayValueWidth + unitGap + unitW + SpeedBps.RIGHT_PAD * drawScale;
        host.setHudSize(boxW, boxH);

        float baseX = host.hudX();
        float baseY = host.hudY();
        float iconY = baseY + (boxH - iconSize) * 0.5f;
        float dividerX = baseX + SpeedBps.DIVIDER_X * drawScale;
        float dividerY = baseY + SpeedBps.DIVIDER_Y * drawScale;
        float dividerW = Math.max(0.5f, SpeedBps.DIVIDER_WIDTH * drawScale);
        float dividerH = Math.max(0.5f, boxH - 2f * SpeedBps.DIVIDER_Y * drawScale);
        float textY = baseY + (boxH - Math.max(valueH, unitH)) * 0.5f;
        float unitX = valueX + host.displayValueWidth + unitGap;

        int iconColor = host.uiIconColor;
        int dividerColor = host.uiMetaColor;
        int valueColor = host.uiValueColor;
        int unitColor = host.uiMetaColor;
        boolean useLabelEffect = host.labelEffect.get() != HudTextEffects.Effect.NONE;
        float time = useLabelEffect ? (float) (Util.getMillis() / 1000.0) : 0.0f;
        if (useLabelEffect) {
            iconColor = HudTextEffects.animatedColor(iconColor, host.labelEffect.get(),
                    host.labelEffectSpeed.get(), time, 0.0f);
            dividerColor = HudTextEffects.animatedColor(dividerColor, host.labelEffect.get(),
                    host.labelEffectSpeed.get(), time, 0.15f);
        }

        float radius = SpeedBps.BOX_RADIUS * drawScale;
        host.scriptModel.setVisible(true);
        host.scriptModel.setRoot(baseX, baseY, boxW, boxH, radius, drawScale);
        host.scriptModel.background().set(host.bgEffect.get(), host.isThemeMode(), host.blurAlpha.get() / 255.0f,
                host.uiBgPrimary, host.uiBgSecondary, host.uiStroke, Math.max(0.5f, SpeedBps.STROKE_WIDTH * drawScale), SpeedBps.BOX_SOFTNESS);
        host.scriptModel.icon().texture(SpeedBps.SPEED_ICON.toString(), iconX - baseX, iconY - baseY, iconSize, iconSize, iconColor);
        host.scriptModel.divider().set(dividerX - baseX, dividerY - baseY, dividerW, dividerH,
                HudRenderUtil.setAlpha(dividerColor, 0x52));
        host.scriptModel.value().set(value, "Onest", drawScale, valueX - baseX, textY - baseY, host.displayValueWidth, valueColor);
        host.scriptModel.unit().set(unit, "OnestMedium", drawScale, unitX - baseX, textY - baseY, unitW, unitColor);
        host.scriptModel.extra().hidden();
        host.scriptModel.animation().setLabelEffect(host.labelEffect.get().name(), host.labelEffectSpeed.get(), time)
                .setDigitAnimation(false, "", 1.0f, 0.0f);
        host.scriptModel.data("bps", currentBps)
                .data("displayedBps", host.displayedSpeed)
                .data("average", useAverage)
                .data("useBps", showBps)
                .data("valueText", value)
                .data("unitText", unit);
        ScriptedCompactHudStatRenderer.INSTANCE.render(host.scriptModel, renderer, fallback, ctx, tickDelta);
    }

    static float smoothWidth(float current, float target) {
        if (current < 0.0f) return target;
        if (target >= current) return target;
        float next = AnimationUtility.approach(current, target, AnimationUtility.deltaTime(), 12.0f);
        return AnimationUtility.snap(next, target, 0.25f);
    }
}
