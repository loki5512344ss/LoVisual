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
import dev.loki.lovisual.features.module.modules.visuals.weather.WorldTweaks;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

final class GameTimeRenderer {

    private GameTimeRenderer() {
    }

    static void render(GameTime host,
                       Renderer2D renderer,
                       TextRenderer textRenderer,
                       GuiGraphicsExtractor ctx,
                       float tickDelta,
                       int screenW,
                       int screenH) {
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        Minecraft mc = Minecraft.getInstance();
        if ((mc == null || mc.level == null) && !preview) {
            host.setHudSize(0f, 0f);
            host.scriptModel.setVisible(false);
            return;
        }
        if (!preview && !host.isEnabled()) {
            host.setHudSize(0f, 0f);
            host.scriptModel.setVisible(false);
            return;
        }

        long worldTime = mc != null && mc.level != null ? (WorldTweaks.getServerTimeOfDay() % 24000L) : 6000L;
        String icon = GameTimeIcons.iconString(GameTimeIcons.resolveIcon(host, worldTime));
        String timeText = GameTimeIcons.formatTime(worldTime);
        GameTimePalette.update(host);

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer iconRenderer = GameTimeIcons.getWeatherIcons();
        if (iconRenderer == null) iconRenderer = fallback;
        TextRenderer valueRenderer = Fonts.renderer("Onest", FontInfo.Type.Regular, fallback);

        float drawScale = HudScale.scale(screenW, screenH)
                * (host.hud.getFontSize() / 18f)
                * GameTime.SCALE_MULT
                * host.scale.get().floatValue();

        float iconScale = drawScale * GameTime.ICON_SCALE;
        iconRenderer.begin(iconScale, true, false);
        float iconW = (float) iconRenderer.getWidth(icon, false);
        float iconH = (float) iconRenderer.getHeight(false);
        iconRenderer.end();

        valueRenderer.begin(drawScale, true, false);
        float timeW = (float) valueRenderer.getWidth(timeText, false);
        float timeH = (float) valueRenderer.getHeight(false);
        float reservedTimeW = (float) valueRenderer.getWidth(GameTime.WIDTH_TEMPLATE, false);
        valueRenderer.end();

        host.displayTimeWidth = Math.max(smoothWidth(host.displayTimeWidth, timeW), reservedTimeW);
        float boxH = GameTime.BOX_HEIGHT * drawScale;
        float boxW = GameTime.TEXT_LEFT_X * drawScale + host.displayTimeWidth + GameTime.RIGHT_PAD * drawScale;
        host.setHudSize(boxW, boxH);

        float baseX = host.hudX();
        float baseY = host.hudY();
        float iconX = baseX + GameTime.ICON_X * drawScale;
        float iconY = baseY + (boxH - iconH) * 0.5f;
        float dividerX = baseX + GameTime.DIVIDER_X * drawScale;
        float dividerY = baseY + GameTime.DIVIDER_Y * drawScale;
        float dividerW = Math.max(0.5f, GameTime.DIVIDER_WIDTH * drawScale);
        float dividerH = Math.max(0.5f, boxH - 2f * GameTime.DIVIDER_Y * drawScale);
        float textX = baseX + GameTime.TEXT_LEFT_X * drawScale;
        float textY = baseY + (boxH - timeH) * 0.5f;

        int resolvedIconColor = host.uiIconColor;
        boolean useIconEffect = host.iconEffect.get() != HudTextEffects.Effect.NONE;
        float nowSec = useIconEffect ? (float) (Util.getMillis() / 1000.0) : 0.0f;
        if (useIconEffect) {
            resolvedIconColor = HudTextEffects.animatedColor(resolvedIconColor, host.iconEffect.get(),
                    host.iconEffectSpeed.get(), nowSec, 0.0f);
        }

        float radius = GameTime.BOX_RADIUS * drawScale;
        host.scriptModel.setVisible(true);
        host.scriptModel.setRoot(baseX, baseY, boxW, boxH, radius, drawScale);
        host.scriptModel.background().set(host.bgEffect.get(), host.isThemeMode(), host.blurAlpha.get() / 255.0f,
                host.uiBgPrimary, host.uiBgSecondary, host.uiStroke, Math.max(0.5f, GameTime.STROKE_WIDTH * drawScale), GameTime.BOX_SOFTNESS);
        host.scriptModel.icon().glyph(icon, "WeatherIcons", iconX - baseX, iconY - baseY, iconW, iconH, iconScale, resolvedIconColor);
        host.scriptModel.divider().set(dividerX - baseX, dividerY - baseY, dividerW, dividerH,
                HudRenderUtil.setAlpha(host.uiValueColor, 0x52));
        host.scriptModel.value().set(timeText, "Onest", drawScale, textX - baseX, textY - baseY, host.displayTimeWidth, host.uiValueColor);
        host.scriptModel.unit().hidden();
        host.scriptModel.extra().hidden();
        host.scriptModel.animation().setLabelEffect(host.iconEffect.get().name(), host.iconEffectSpeed.get(), nowSec)
                .setDigitAnimation(false, "", 1.0f, 0.0f);
        host.scriptModel.data("worldTime", worldTime)
                .data("timeText", timeText)
                .data("weatherIcon", icon)
                .data("showWeather", host.showWeather.get());
        ScriptedCompactHudStatRenderer.INSTANCE.render(host.scriptModel, renderer, fallback, ctx, tickDelta);
    }

    static float smoothWidth(float current, float target) {
        if (current < 0.0f) return target;
        if (target >= current) return target;
        float next = AnimationUtility.approach(current, target, AnimationUtility.deltaTime(), 12.0f);
        return AnimationUtility.snap(next, target, 0.25f);
    }
}
