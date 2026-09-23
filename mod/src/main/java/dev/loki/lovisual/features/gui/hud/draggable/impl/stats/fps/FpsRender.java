/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.stats;

import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.script.ScriptedCompactHudStatRenderer;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.FastFps;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

final class FpsRender {

    private FpsRender() {
    }

    static void render(Fps host,
                       Renderer2D renderer,
                       TextRenderer textRenderer,
                       GuiGraphicsExtractor ctx,
                       float tickDelta,
                       int screenW,
                       int screenH) {
        int fps = Math.max(0, FastFps.getFps());
        long nowMs = Util.getMillis();
        String value = FpsAnimator.resolveDisplayedFps(host, Integer.toString(fps), nowMs);
        String unit = "FPS";
        float digitAnimProgress = FpsAnimator.digitProgress(host, nowMs);
        String widthValue = host.digitAnimation.get() ? FpsAnimator.widerText(value, host.previousShownFps) : value;
        FpsPalette.update(host);

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer valueRenderer = Fonts.renderer("Onest", FontInfo.Type.Regular, fallback);
        TextRenderer metaRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, valueRenderer);

        float drawScale = HudScale.scale(screenW, screenH)
                * (host.hud.getFontSize() / 18f)
                * Fps.SCALE_MULT
                * host.scale.get().floatValue();

        valueRenderer.begin(drawScale, true, false);
        float valueW = (float) valueRenderer.getWidth(widthValue, false);
        float valueH = (float) valueRenderer.getHeight(false);
        valueRenderer.end();

        metaRenderer.begin(drawScale, true, false);
        float unitW = (float) metaRenderer.getWidth(unit, false);
        float unitH = (float) metaRenderer.getHeight(false);
        metaRenderer.end();

        float boxH = Fps.BOX_HEIGHT * drawScale;
        float iconSize = Fps.ICON_SIZE * drawScale;
        float valueX = host.hudX() + Fps.TEXT_LEFT_X * drawScale;
        float unitGap = Fps.VALUE_UNIT_GAP * drawScale;
        host.displayValueWidth = FpsAnimator.smoothWidth(host.displayValueWidth, valueW);
        float boxW = (valueX - host.hudX()) + host.displayValueWidth + unitGap + unitW + Fps.RIGHT_PAD * drawScale;
        host.setHudSize(boxW, boxH);

        float baseX = host.hudX();
        float baseY = host.hudY();
        float iconX = baseX + Fps.ICON_X * drawScale;
        float iconY = baseY + (boxH - iconSize) * 0.5f;
        float dividerX = baseX + Fps.DIVIDER_X * drawScale;
        float dividerY = baseY + Fps.DIVIDER_Y * drawScale;
        float dividerW = Math.max(0.5f, Fps.DIVIDER_WIDTH * drawScale);
        float dividerH = Math.max(0.5f, boxH - 2f * Fps.DIVIDER_Y * drawScale);
        float textY = baseY + (boxH - Math.max(valueH, unitH)) * 0.5f;
        float unitX = valueX + host.displayValueWidth + unitGap;

        int resolvedIconColor = host.uiIconColor;
        boolean useLabelEffect = host.labelEffect.get() != HudTextEffects.Effect.NONE;
        float time = useLabelEffect ? (float) (Util.getMillis() / 1000.0) : 0.0f;
        if (useLabelEffect) {
            resolvedIconColor = HudTextEffects.animatedColor(resolvedIconColor, host.labelEffect.get(),
                    host.labelEffectSpeed.get(), time, 0.0f);
        }

        float radius = Fps.BOX_RADIUS * drawScale;
        host.scriptModel.setVisible(true);
        host.scriptModel.setRoot(baseX, baseY, boxW, boxH, radius, drawScale);
        host.scriptModel.background().set(host.bgEffect.get(), FpsPalette.isThemeMode(host), host.blurAlpha.get() / 255.0f,
                host.uiBgPrimary, host.uiBgSecondary, host.uiStroke, Math.max(0.5f, Fps.STROKE_WIDTH * drawScale), Fps.BOX_SOFTNESS);
        host.scriptModel.icon().texture(Fps.FPS_ICON.toString(), iconX - baseX, iconY - baseY, iconSize, iconSize, resolvedIconColor);
        host.scriptModel.divider().set(dividerX - baseX, dividerY - baseY, dividerW, dividerH,
                HudRenderUtil.setAlpha(host.uiMetaColor, 0x52));
        host.scriptModel.value().set(value, "Onest", drawScale, valueX - baseX, textY - baseY, host.displayValueWidth, host.uiValueColor);
        host.scriptModel.unit().set(unit, "OnestMedium", drawScale, unitX - baseX, textY - baseY, unitW, host.uiMetaColor);
        host.scriptModel.extra().hidden();
        host.scriptModel.animation().setLabelEffect(host.labelEffect.get().name(), host.labelEffectSpeed.get(), time)
                .setDigitAnimation(host.digitAnimation.get(), host.previousShownFps, digitAnimProgress, Fps.DIGIT_ANIMATION_OFFSET);
        host.scriptModel.data("fps", fps)
                .data("valueText", value)
                .data("unitText", unit);
        ScriptedCompactHudStatRenderer.INSTANCE.render(host.scriptModel, renderer, fallback, ctx, tickDelta);
    }
}
