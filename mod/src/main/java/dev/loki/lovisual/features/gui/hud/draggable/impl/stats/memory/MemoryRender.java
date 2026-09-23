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
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

final class MemoryRender {

    private MemoryRender() {
    }

    static void render(Memory host,
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

        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        long used = Math.max(0L, total - free);
        long usedMiB = MemoryFormat.toMiB(used);
        long maxMiB = MemoryFormat.toMiB(max > 0L ? max : total);
        int percent = max > 0L ? (int) Math.round((double) used * 100.0 / (double) max) : 0;
        percent = Math.max(0, Math.min(100, percent));
        MemoryFormat.updateMemoryStrings(host, usedMiB, maxMiB, percent);
        MemoryPalette.update(host);

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer valueRenderer = Fonts.renderer("Onest", FontInfo.Type.Regular, fallback);
        TextRenderer metaRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, valueRenderer);

        float drawScale = HudScale.scale(screenW, screenH)
                * (host.hud.getFontSize() / 18f)
                * Memory.SCALE_MULT
                * host.scale.get().floatValue();

        valueRenderer.begin(drawScale, true, false);
        float valueW = (float) valueRenderer.getWidth(host.cachedValue, false);
        float valueH = (float) valueRenderer.getHeight(false);
        valueRenderer.end();

        metaRenderer.begin(drawScale, true, false);
        float unitW = (float) metaRenderer.getWidth("MB", false);
        float unitH = (float) metaRenderer.getHeight(false);
        float extraW = (float) metaRenderer.getWidth(host.cachedExtra, false);
        metaRenderer.end();

        float boxH = Memory.BOX_HEIGHT * drawScale;
        float iconSize = Memory.ICON_SIZE * drawScale;
        float valueX = host.hudX() + Memory.TEXT_LEFT_X * drawScale;
        float unitGap = Memory.VALUE_UNIT_GAP * drawScale;
        float extraGap = Memory.EXTRA_GAP * drawScale;
        host.displayValueWidth = smoothWidth(host.displayValueWidth, valueW);
        host.displayExtraWidth = smoothWidth(host.displayExtraWidth, extraW);
        float boxW = (valueX - host.hudX())
                + host.displayValueWidth
                + unitGap
                + unitW
                + extraGap
                + host.displayExtraWidth
                + Memory.RIGHT_PAD * drawScale;
        host.setHudSize(boxW, boxH);

        float baseX = host.hudX();
        float baseY = host.hudY();
        float iconX = baseX + Memory.ICON_X * drawScale;
        float iconY = baseY + (boxH - iconSize) * 0.5f;
        float dividerX = baseX + Memory.DIVIDER_X * drawScale;
        float dividerY = baseY + Memory.DIVIDER_Y * drawScale;
        float dividerW = Math.max(0.5f, Memory.DIVIDER_WIDTH * drawScale);
        float dividerH = Math.max(0.5f, boxH - 2f * Memory.DIVIDER_Y * drawScale);
        float textY = baseY + (boxH - Math.max(valueH, unitH)) * 0.5f;
        float unitX = valueX + host.displayValueWidth + unitGap;
        float extraX = unitX + unitW + extraGap;

        int resolvedIconColor = host.uiIconColor;
        boolean useLabelEffect = host.labelEffect.get() != HudTextEffects.Effect.NONE;
        float time = useLabelEffect ? (float) (Util.getMillis() / 1000.0) : 0.0f;
        if (useLabelEffect) {
            resolvedIconColor = HudTextEffects.animatedColor(resolvedIconColor, host.labelEffect.get(),
                    host.labelEffectSpeed.get(), time, 0.0f);
        }

        float radius = Memory.BOX_RADIUS * drawScale;
        host.scriptModel.setVisible(true);
        host.scriptModel.setRoot(baseX, baseY, boxW, boxH, radius, drawScale);
        host.scriptModel.background().set(host.bgEffect.get(), host.isThemeMode(), host.blurAlpha.get() / 255.0f,
                host.uiBgPrimary, host.uiBgSecondary, host.uiStroke, Math.max(0.5f, Memory.STROKE_WIDTH * drawScale), Memory.BOX_SOFTNESS);
        host.scriptModel.icon().texture(Memory.MEMORY_ICON.toString(), iconX - baseX, iconY - baseY, iconSize, iconSize, resolvedIconColor);
        host.scriptModel.divider().set(dividerX - baseX, dividerY - baseY, dividerW, dividerH,
                HudRenderUtil.setAlpha(host.uiMetaColor, 0x52));
        host.scriptModel.value().set(host.cachedValue, "Onest", drawScale, valueX - baseX, textY - baseY, host.displayValueWidth, host.uiValueColor);
        host.scriptModel.unit().set("MB", "OnestMedium", drawScale, unitX - baseX, textY - baseY, unitW, host.uiMetaColor);
        host.scriptModel.extra().set(host.cachedExtra, "OnestMedium", drawScale, extraX - baseX, textY - baseY, host.displayExtraWidth, host.uiExtraColor);
        host.scriptModel.animation().setLabelEffect(host.labelEffect.get().name(), host.labelEffectSpeed.get(), time)
                .setDigitAnimation(false, "", 1.0f, 0.0f);
        host.scriptModel.data("usedMiB", usedMiB)
                .data("maxMiB", maxMiB)
                .data("percent", percent)
                .data("valueText", host.cachedValue)
                .data("unitText", "MB")
                .data("extraText", host.cachedExtra);
        ScriptedCompactHudStatRenderer.INSTANCE.render(host.scriptModel, renderer, fallback, ctx, tickDelta);
    }

    static float smoothWidth(float current, float target) {
        if (current < 0.0f) return target;
        if (target >= current) return target;
        float next = AnimationUtility.approach(current, target, AnimationUtility.deltaTime(), 12.0f);
        return AnimationUtility.snap(next, target, 0.25f);
    }
}
