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
import net.minecraft.world.level.Level;

final class CoordinatesRenderer {

    private CoordinatesRenderer() {
    }

    static void render(Coordinates host,
                       Renderer2D renderer,
                       TextRenderer textRenderer,
                       GuiGraphicsExtractor ctx,
                       float tickDelta,
                       int screenW,
                       int screenH) {
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || (mc.player == null && !preview)) {
            host.setHudSize(0f, 0f);
            host.scriptModel.setVisible(false);
            return;
        }
        if (!preview && !host.isEnabled()) {
            host.setHudSize(0f, 0f);
            host.scriptModel.setVisible(false);
            return;
        }

        boolean inNether = mc.level != null && mc.level.dimension() == Level.NETHER;
        double rawX = mc.player != null ? mc.player.getX() : 0.0;
        double rawY = mc.player != null ? mc.player.getY() : 0.0;
        double rawZ = mc.player != null ? mc.player.getZ() : 0.0;
        int px = Mth.floor(rawX);
        int py = Mth.floor(rawY);
        int pz = Mth.floor(rawZ);
        boolean showNether = host.shouldShowNether(inNether);
        int hx = showNether ? (int) (rawX * (inNether ? 8.0 : 0.125)) : 0;
        int hz = showNether ? (int) (rawZ * (inNether ? 8.0 : 0.125)) : 0;
        CoordinatesPalette.update(host);

        String xValue = Integer.toString(px);
        String yValue = Integer.toString(py);
        String zValue = Integer.toString(pz);
        String extra = showNether ? "[" + hx + " " + hz + "]" : "";

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer tr = Fonts.renderer("Onest", FontInfo.Type.Regular, fallback);

        float drawScale = HudScale.scale(screenW, screenH)
                * (host.hud.getFontSize() / 18f)
                * Coordinates.SCALE_MULT
                * host.scale.get().floatValue();

        tr.begin(drawScale, true, false);
        float labelXW = (float) tr.getWidth("x", false);
        float labelYW = (float) tr.getWidth("y", false);
        float labelZW = (float) tr.getWidth("z", false);
        float valueXW = (float) tr.getWidth(xValue, false);
        float valueYW = (float) tr.getWidth(yValue, false);
        float valueZW = (float) tr.getWidth(zValue, false);
        float extraW = showNether ? (float) tr.getWidth(extra, false) : 0f;
        float textH = (float) tr.getHeight(false);
        tr.end();

        float boxH = Coordinates.BOX_HEIGHT * drawScale;
        float iconSize = Coordinates.ICON_SIZE * drawScale;
        float cursorX = host.hudX() + Coordinates.TEXT_LEFT_X * drawScale;
        float gap = Coordinates.SEGMENT_GAP * drawScale;
        float pairGap = Coordinates.LABEL_VALUE_GAP * drawScale;
        host.displayXValueWidth = smoothWidth(host.displayXValueWidth, valueXW);
        host.displayYValueWidth = smoothWidth(host.displayYValueWidth, valueYW);
        host.displayZValueWidth = smoothWidth(host.displayZValueWidth, valueZW);
        host.displayExtraWidth = showNether ? smoothWidth(host.displayExtraWidth, extraW) : 0.0f;
        float boxW = (cursorX - host.hudX())
                + labelXW + pairGap + host.displayXValueWidth
                + gap
                + labelYW + pairGap + host.displayYValueWidth
                + gap
                + labelZW + pairGap + host.displayZValueWidth
                + (showNether ? gap + host.displayExtraWidth : 0f)
                + Coordinates.RIGHT_PAD * drawScale;

        host.setHudSize(boxW, boxH);

        float baseX = host.hudX();
        float baseY = host.hudY();
        float iconX = baseX + Coordinates.ICON_X * drawScale;
        float iconY = baseY + (boxH - iconSize) * 0.5f;
        float dividerX = baseX + Coordinates.DIVIDER_X * drawScale;
        float dividerY = baseY + Coordinates.DIVIDER_Y * drawScale;
        float dividerW = Math.max(0.5f, Coordinates.DIVIDER_WIDTH * drawScale);
        float dividerH = Math.max(0.5f, boxH - 2f * Coordinates.DIVIDER_Y * drawScale);
        float textY = baseY + (boxH - textH) * 0.5f;

        int iconColor = host.uiIconColor;
        boolean useLabelEffect = host.labelEffect.get() != HudTextEffects.Effect.NONE;
        float time = useLabelEffect ? (float) (Util.getMillis() / 1000.0) : 0.0f;
        if (useLabelEffect) {
            iconColor = HudTextEffects.animatedColor(iconColor, host.labelEffect.get(),
                    host.labelEffectSpeed.get(), time, 0.0f);
        }

        float radius = Coordinates.BOX_RADIUS * drawScale;
        host.scriptModel.setVisible(true);
        host.scriptModel.setRoot(baseX, baseY, boxW, boxH, radius, drawScale);
        host.scriptModel.background().set(host.bgEffect.get(), host.isThemeMode(), host.blurAlpha.get() / 255.0f,
                host.uiBgPrimary, host.uiBgSecondary, host.uiStroke, Math.max(0.5f, Coordinates.STROKE_WIDTH * drawScale), Coordinates.BOX_SOFTNESS);
        host.scriptModel.icon().texture(Coordinates.COORDS_ICON.toString(), iconX - baseX, iconY - baseY, iconSize, iconSize, iconColor);
        host.scriptModel.divider().set(dividerX - baseX, dividerY - baseY, dividerW, dividerH,
                HudRenderUtil.setAlpha(host.uiLabelColor, 0x52));
        host.scriptModel.value().set(xValue + " " + yValue + " " + zValue, "Onest", drawScale,
                cursorX - baseX, textY - baseY, boxW - (cursorX - baseX), host.uiValueColor);
        host.scriptModel.unit().hidden();
        if (showNether) {
            host.scriptModel.extra().set(extra, "OnestMedium", drawScale,
                    boxW - Coordinates.RIGHT_PAD * drawScale - host.displayExtraWidth, textY - baseY, host.displayExtraWidth, host.uiExtraColor);
        } else {
            host.scriptModel.extra().hidden();
        }
        host.scriptModel.animation().setLabelEffect(host.labelEffect.get().name(), host.labelEffectSpeed.get(), time)
                .setDigitAnimation(false, "", 1.0f, 0.0f);
        host.scriptModel.data("x", px)
                .data("y", py)
                .data("z", pz)
                .data("xText", xValue)
                .data("yText", yValue)
                .data("zText", zValue)
                .data("showNether", showNether)
                .data("netherX", hx)
                .data("netherZ", hz)
                .data("netherText", extra)
                .data("labelXW", labelXW)
                .data("labelYW", labelYW)
                .data("labelZW", labelZW)
                .data("valueXW", host.displayXValueWidth)
                .data("valueYW", host.displayYValueWidth)
                .data("valueZW", host.displayZValueWidth)
                .data("xyzExtraW", host.displayExtraWidth)
                .data("labelColor", dev.loki.lovisual.features.gui.hud.script.CompactHudStatModel.colorString(host.uiLabelColor))
                .data("valueColor", dev.loki.lovisual.features.gui.hud.script.CompactHudStatModel.colorString(host.uiValueColor));
        ScriptedCompactHudStatRenderer.INSTANCE.render(host.scriptModel, renderer, fallback, ctx, tickDelta);
    }

    static float smoothWidth(float current, float target) {
        if (current < 0.0f) return target;
        if (target >= current) return target;
        float next = AnimationUtility.approach(current, target, AnimationUtility.deltaTime(), 12.0f);
        return AnimationUtility.snap(next, target, 0.25f);
    }
}
