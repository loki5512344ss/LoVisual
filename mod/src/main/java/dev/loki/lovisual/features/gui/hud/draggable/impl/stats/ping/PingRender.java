/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.stats;

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
import dev.loki.lovisual.util.player.interaction.NetworkStatsUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

final class PingRender {

    private PingRender() {
    }

    static void render(Ping host,
                       Renderer2D renderer,
                       TextRenderer textRenderer,
                       GuiGraphicsExtractor ctx,
                       float tickDelta,
                       int screenW,
                       int screenH) {
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        if (Minecraft.getInstance() == null && !preview) {
            host.setHudSize(0f, 0f);
            host.scriptModel.setVisible(false);
            return;
        }
        if (!preview && !host.isEnabled()) {
            host.setHudSize(0f, 0f);
            host.scriptModel.setVisible(false);
            return;
        }

        int ping = NetworkStatsUtil.getPing(Minecraft.getInstance());
        String value = PingFormat.getPingValue(host, ping);
        String unit = "ms";
        PingPalette.update(host);

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer valueRenderer = Fonts.renderer("Onest", FontInfo.Type.Regular, fallback);
        TextRenderer metaRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, valueRenderer);

        float drawScale = HudScale.scale(screenW, screenH)
                * (host.hud.getFontSize() / 18f)
                * Ping.SCALE_MULT
                * host.scale.get().floatValue();

        valueRenderer.begin(drawScale, true, false);
        float valueW = (float) valueRenderer.getWidth(value, false);
        float valueH = (float) valueRenderer.getHeight(false);
        valueRenderer.end();

        metaRenderer.begin(drawScale, true, false);
        float unitW = (float) metaRenderer.getWidth(unit, false);
        float unitH = (float) metaRenderer.getHeight(false);
        metaRenderer.end();

        float boxH = Ping.BOX_HEIGHT * drawScale;
        float iconSize = Ping.ICON_SIZE * drawScale;
        float valueX = host.hudX() + Ping.TEXT_LEFT_X * drawScale;
        float unitGap = Ping.VALUE_UNIT_GAP * drawScale;
        host.displayValueWidth = smoothWidth(host.displayValueWidth, valueW);
        float boxW = (valueX - host.hudX()) + host.displayValueWidth + unitGap + unitW + Ping.RIGHT_PAD * drawScale;
        host.setHudSize(boxW, boxH);

        float baseX = host.hudX();
        float baseY = host.hudY();
        float iconX = baseX + Ping.ICON_X * drawScale;
        float iconY = baseY + (boxH - iconSize) * 0.5f;
        float dividerX = baseX + Ping.DIVIDER_X * drawScale;
        float dividerY = baseY + Ping.DIVIDER_Y * drawScale;
        float dividerW = Math.max(0.5f, Ping.DIVIDER_WIDTH * drawScale);
        float dividerH = Math.max(0.5f, boxH - 2f * Ping.DIVIDER_Y * drawScale);
        float textY = baseY + (boxH - Math.max(valueH, unitH)) * 0.5f;
        float unitX = valueX + host.displayValueWidth + unitGap;

        int resolvedIconColor = host.uiIconColor;
        boolean useLabelEffect = host.labelEffect.get() != HudTextEffects.Effect.NONE;
        float time = useLabelEffect ? (float) (Util.getMillis() / 1000.0) : 0.0f;
        if (useLabelEffect) {
            resolvedIconColor = HudTextEffects.animatedColor(resolvedIconColor, host.labelEffect.get(),
                    host.labelEffectSpeed.get(), time, 0.0f);
        }

        float radius = Ping.BOX_RADIUS * drawScale;
        host.scriptModel.setVisible(true);
        host.scriptModel.setRoot(baseX, baseY, boxW, boxH, radius, drawScale);
        host.scriptModel.background().set(host.bgEffect.get(), host.isThemeMode(), host.blurAlpha.get() / 255.0f,
                host.uiBgPrimary, host.uiBgSecondary, host.uiStroke, Math.max(0.5f, Ping.STROKE_WIDTH * drawScale), Ping.BOX_SOFTNESS);
        host.scriptModel.icon().texture(Ping.PING_ICON.toString(), iconX - baseX, iconY - baseY, iconSize, iconSize, resolvedIconColor);
        host.scriptModel.divider().set(dividerX - baseX, dividerY - baseY, dividerW, dividerH,
                HudRenderUtil.setAlpha(host.uiMetaColor, 0x52));
        host.scriptModel.value().set(value, "Onest", drawScale, valueX - baseX, textY - baseY, host.displayValueWidth, host.uiValueColor);
        host.scriptModel.unit().set(unit, "OnestMedium", drawScale, unitX - baseX, textY - baseY, unitW, host.uiMetaColor);
        host.scriptModel.extra().hidden();
        host.scriptModel.animation().setLabelEffect(host.labelEffect.get().name(), host.labelEffectSpeed.get(), time)
                .setDigitAnimation(false, "", 1.0f, 0.0f);
        host.scriptModel.data("ping", ping)
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
