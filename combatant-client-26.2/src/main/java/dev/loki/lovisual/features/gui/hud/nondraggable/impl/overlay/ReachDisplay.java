/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl.overlay;

import dev.loki.lovisual.config.values.primitive.BooleanValue;
import dev.loki.lovisual.config.values.primitive.NumberValue;
import dev.loki.lovisual.config.values.color.RGBColorValue;
import dev.loki.lovisual.features.gui.hud.base.AbstractHudElement;
import dev.loki.lovisual.features.gui.hud.registry.HudElementRegister;
import dev.loki.lovisual.features.gui.hud.anchor.HudRenderSpace;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.combat.tracking.ReachMeter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static dev.loki.lovisual.features.theme.Theme.theme;

/**
 * Shows the reach (eye -> target hitbox distance) of your last melee hit just
 * below the crosshair, then fades out. Pure information widget: it never
 * changes attack range.
 */
@HudElementRegister(order = 35)
public final class ReachDisplay extends AbstractHudElement {

    public static final ReachDisplay INSTANCE = new ReachDisplay();

    private static final float PAD_X = 6f;
    private static final float PAD_Y = 4f;
    private static final float RADIUS = 5f;
    private static final float CROSSHAIR_GAP = 26f;
    private static final float FADE_WINDOW_MS = 1800f;
    private static final int OVERREACH_TINT = 0xFFFF6368;

    private final NumberValue<Double> scale = num("reach_scale", "scale", 1.0, 0.5, 2.5);
    private final NumberValue<Integer> keepMs = num("reach_keep_ms", "keep_ms", 2500, 500, 10000);
    private final BooleanValue syncTheme = bool("reach_sync_theme", "sync_theme", true);
    private final BooleanValue showMax = bool("reach_show_max", "show_max", true);
    private final BooleanValue highlightOverreach = bool("reach_highlight_overreach", "highlight_overreach", true);
    private final RGBColorValue valueColor =
            visibleWhen(colorNoAlpha("reach_value_color", "#5CC8E7"), () -> !syncTheme.get());
    private final RGBColorValue bgColor =
            visibleWhen(colorNoAlpha("reach_bg_color", "#1A1D23"), () -> !syncTheme.get());

    private ReachDisplay() {
        super("reach_display", "Reach", true);
    }

    public static ReachDisplay get() {
        return INSTANCE;
    }

    @Override
    public boolean usesEngineRenderer() {
        return true;
    }

    @Override
    public HudRenderSpace getRenderSpace() {
        return HudRenderSpace.UNSCALED_LOGICAL;
    }

    @Override
    public void renderEngine(Renderer2D renderer,
                             TextRenderer textRenderer,
                             GuiGraphicsExtractor ctx,
                             float tickDelta,
                             int screenW,
                             int screenH) {
        double distance = ReachMeter.lastDistance();
        Minecraft mc = Minecraft.getInstance();
        if (distance < 0.0 || mc == null || mc.player == null || mc.level == null) {
            width = 0f;
            height = 0f;
            return;
        }

        long age = System.currentTimeMillis() - ReachMeter.lastAttackAtMs();
        if (age > keepMs.get() + FADE_WINDOW_MS) {
            width = 0f;
            height = 0f;
            return;
        }
        int alpha = clampAlpha((int) ((1.0f - Math.max(0f, age - keepMs.get()) / FADE_WINDOW_MS) * 255f));

        String value = ReachMeter.format(distance);
        if (showMax.get()) {
            value = value + "  •  max " + ReachMeter.format(ReachMeter.sessionMax());
        }

        float drawScale = HudScale.scale(screenW, screenH) * scale.get().floatValue();
        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer font = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, fallback);

        font.begin(drawScale, true, false);
        float textW = (float) font.getWidth(value, false);
        float textH = (float) font.getHeight(false);
        font.end();

        float boxW = textW + PAD_X * 2f * drawScale;
        float boxH = textH + PAD_Y * 2f * drawScale;
        float boxX = (screenW - boxW) * 0.5f;
        float boxY = screenH * 0.5f + CROSSHAIR_GAP * drawScale;

        boolean overreach = highlightOverreach.get() && distance > ReachMeter.VANILLA_REACH;
        int valueArgb = overreach ? OVERREACH_TINT
                : syncTheme.get() ? theme().accent() : valueColor.getArgb() | 0xFF000000;
        int bg = syncTheme.get()
                ? HudRenderUtil.setAlpha(theme().windowBg(), 0xCC)
                : (0xCC << 24) | (bgColor.getArgb() & 0x00FFFFFF);

        renderer.roundedRect(boxX, boxY, boxW, boxH, RADIUS * drawScale, 1.0f,
                HudRenderUtil.setAlpha(bg, alpha));

        font.begin(drawScale, true, false);
        font.render(value, boxX + PAD_X * drawScale, boxY + (boxH - textH) * 0.5f,
                new RenderColor(HudRenderUtil.setAlpha(valueArgb, alpha)), false);
        font.end();

        width = boxW;
        height = boxH;
        x = boxX;
        y = boxY;
    }

    private static int clampAlpha(int a) {
        return Math.max(0, Math.min(255, a));
    }
}
