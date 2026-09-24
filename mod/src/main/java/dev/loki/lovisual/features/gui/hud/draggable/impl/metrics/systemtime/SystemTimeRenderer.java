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
import net.minecraft.util.Util;

import java.time.LocalTime;

final class SystemTimeRenderer {

    private SystemTimeRenderer() {
    }

    static void render(SystemTime host,
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

        LocalTime now = LocalTime.now();
        String icon = SystemTimeIcons.clockIcon(now.getHour());
        String timeText = SystemTimeIcons.formatTime(now, host.showSeconds.get());
        SystemTimePalette.update(host);

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer iconRenderer = SystemTimeIcons.getWeatherIcons();
        if (iconRenderer == null) iconRenderer = fallback;
        TextRenderer valueRenderer = Fonts.renderer("Onest", FontInfo.Type.Regular, fallback);

        float drawScale = HudScale.scale(screenW, screenH)
                * (host.hud.getFontSize() / 18f)
                * SystemTime.SCALE_MULT
                * host.scale.get().floatValue();

        float iconScale = drawScale * SystemTime.ICON_SCALE;
        iconRenderer.begin(iconScale, true, false);
        float iconW = (float) iconRenderer.getWidth(icon, false);
        float iconH = (float) iconRenderer.getHeight(false);
        iconRenderer.end();

        valueRenderer.begin(drawScale, true, false);
        float timeW = (float) valueRenderer.getWidth(timeText, false);
        float timeH = (float) valueRenderer.getHeight(false);
        String widthTemplate = host.showSeconds.get() ? SystemTime.WIDTH_TEMPLATE_SECONDS : SystemTime.WIDTH_TEMPLATE;
        float reservedTimeW = (float) valueRenderer.getWidth(widthTemplate, false);
        valueRenderer.end();

        host.displayTimeWidth = Math.max(smoothWidth(host.displayTimeWidth, timeW), reservedTimeW);
        float boxH = SystemTime.BOX_HEIGHT * drawScale;
        float boxW = SystemTime.TEXT_LEFT_X * drawScale + host.displayTimeWidth + SystemTime.RIGHT_PAD * drawScale;
        host.setHudSize(boxW, boxH);

        float baseX = host.hudX();
        float baseY = host.hudY();
        float iconX = baseX + SystemTime.ICON_X * drawScale;
        float iconY = baseY + (boxH - iconH) * 0.5f;
        float dividerX = baseX + SystemTime.DIVIDER_X * drawScale;
        float dividerY = baseY + SystemTime.DIVIDER_Y * drawScale;
        float dividerW = Math.max(0.5f, SystemTime.DIVIDER_WIDTH * drawScale);
        float dividerH = Math.max(0.5f, boxH - 2f * SystemTime.DIVIDER_Y * drawScale);
        float textX = baseX + SystemTime.TEXT_LEFT_X * drawScale;
        float textY = baseY + (boxH - timeH) * 0.5f;

        int resolvedIconColor = host.uiIconColor;
        boolean useIconEffect = host.iconEffect.get() != HudTextEffects.Effect.NONE;
        float nowSec = useIconEffect ? (float) (Util.getMillis() / 1000.0) : 0.0f;
        if (useIconEffect) {
            resolvedIconColor = HudTextEffects.animatedColor(resolvedIconColor, host.iconEffect.get(),
                    host.iconEffectSpeed.get(), nowSec, 0.0f);
        }

        float radius = SystemTime.BOX_RADIUS * drawScale;
        host.scriptModel.setVisible(true);
        host.scriptModel.setRoot(baseX, baseY, boxW, boxH, radius, drawScale);
        host.scriptModel.background().set(host.bgEffect.get(), host.isThemeMode(), host.blurAlpha.get() / 255.0f,
                host.uiBgPrimary, host.uiBgSecondary, host.uiStroke, Math.max(0.5f, SystemTime.STROKE_WIDTH * drawScale), SystemTime.BOX_SOFTNESS);
        host.scriptModel.icon().glyph(icon, "WeatherIcons", iconX - baseX, iconY - baseY, iconW, iconH, iconScale, resolvedIconColor);
        host.scriptModel.divider().set(dividerX - baseX, dividerY - baseY, dividerW, dividerH,
                HudRenderUtil.setAlpha(host.uiValueColor, 0x52));
        host.scriptModel.value().set(timeText, "Onest", drawScale, textX - baseX, textY - baseY, host.displayTimeWidth, host.uiValueColor);
        host.scriptModel.unit().hidden();
        host.scriptModel.extra().hidden();
        host.scriptModel.animation().setLabelEffect(host.iconEffect.get().name(), host.iconEffectSpeed.get(), nowSec)
                .setDigitAnimation(false, "", 1.0f, 0.0f);
        host.scriptModel.data("timeText", timeText)
                .data("clockIcon", icon)
                .data("showSeconds", host.showSeconds.get())
                .data("hour", now.getHour())
                .data("minute", now.getMinute())
                .data("second", now.getSecond());
        ScriptedCompactHudStatRenderer.INSTANCE.render(host.scriptModel, renderer, fallback, ctx, tickDelta);
    }

    static float smoothWidth(float current, float target) {
        if (current < 0.0f) return target;
        if (target >= current) return target;
        float next = AnimationUtility.approach(current, target, AnimationUtility.deltaTime(), 12.0f);
        return AnimationUtility.snap(next, target, 0.25f);
    }
}
