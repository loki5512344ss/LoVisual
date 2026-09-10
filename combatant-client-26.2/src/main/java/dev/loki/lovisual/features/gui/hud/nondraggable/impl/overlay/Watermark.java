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
import dev.loki.lovisual.util.FastFps;
import dev.loki.lovisual.util.player.interaction.NetworkStatsUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static dev.loki.lovisual.features.theme.Theme.theme;

/**
 * LoVisual watermark: a small brand pill in the top-left corner with optional
 * FPS and ping segments. Static UI element (Settings -> UI), theme-synced colors,
 * not a copy of the SoupVisuals component stack.
 */
@HudElementRegister(order = 30)
public final class Watermark extends AbstractHudElement {

    public static final Watermark INSTANCE = new Watermark();

    private static final float PAD_X = 6f;
    private static final float PAD_Y = 4f;
    private static final float GAP = 5f;
    private static final float DOT_SIZE = 3f;
    private static final float RADIUS = 5f;
    private static final float ORIGIN_X = 4f;
    private static final float ORIGIN_Y = 4f;

    private final NumberValue<Double> scale = num("watermark_scale", "scale", 1.0, 0.5, 2.5);
    private final BooleanValue syncTheme = bool("watermark_sync_theme", "sync_theme", true);
    private final BooleanValue showVersion = bool("watermark_show_version", "show_version", true);
    private final BooleanValue showUser = bool("watermark_show_user", "show_user", false);
    private final BooleanValue showFps = bool("watermark_show_fps", "show_fps", true);
    private final BooleanValue showPing = bool("watermark_show_ping", "show_ping", true);
    private final RGBColorValue titleColor =
            visibleWhen(colorNoAlpha("watermark_title_color", "title_color", "#5CC8E7"), () -> !syncTheme.get());
    private final RGBColorValue metaColor =
            visibleWhen(colorNoAlpha("watermark_meta_color", "meta_color", "#9B9B9B"), () -> !syncTheme.get());
    private final RGBColorValue bgColor =
            visibleWhen(colorNoAlpha("watermark_bg_color", "bg_color", "#1A1D23"), () -> !syncTheme.get());

    private Watermark() {
        super("watermark", "Watermark", true);
    }

    public static Watermark get() {
        return INSTANCE;
    }

    public static String brandLine() {
        return FabricLoader.getInstance().getModContainer("lovisual")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("");
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
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        String brand = "LoVisual";
        String version = showVersion.get() ? brandLine() : "";
        String fps = showFps.get() ? Math.max(0, FastFps.getFps()) + " fps" : null;
        String ping = null;
        if (showPing.get() && mc != null) {
            int pingMs = NetworkStatsUtil.getPing(mc);
            if (pingMs >= 0) ping = pingMs + " ms";
        }

        String meta = joinMeta(
                version,
                showUser.get() && mc != null && mc.getUser() != null ? mc.getUser().getName() : null,
                fps,
                ping);
        float drawScale = HudScale.scale(screenW, screenH) * scale.get().floatValue();

        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer titleFont = Fonts.renderer("OnestMedium", FontInfo.Type.Bold, fallback);
        TextRenderer metaFont = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, fallback);

        titleFont.begin(drawScale, true, false);
        float brandW = (float) titleFont.getWidth(brand, false);
        float textH = (float) titleFont.getHeight(false);
        titleFont.end();

        float metaW = 0f;
        if (!meta.isEmpty()) {
            metaFont.begin(drawScale, true, false);
            metaW = (float) metaFont.getWidth(meta, false);
            metaFont.end();
        }

        float dotW = meta.isEmpty() ? 0f : DOT_SIZE * drawScale + GAP * drawScale;
        float boxW = (brandW + dotW + metaW) + PAD_X * 2f * drawScale;
        float boxH = textH + PAD_Y * 2f * drawScale;
        float radius = RADIUS * drawScale;

        int bg = resolveBg();
        int titleArgb = syncTheme.get() ? theme().accent() : titleColor.getArgb() | 0xFF000000;
        int metaArgb = syncTheme.get() ? theme().textMuted() : metaColor.getArgb() | 0xFF000000;

        renderer.roundedRect(ORIGIN_X, ORIGIN_Y, boxW, boxH, radius, 1.0f, bg);

        float cursorX = ORIGIN_X + PAD_X * drawScale;
        float textY = ORIGIN_Y + (boxH - textH) * 0.5f;

        titleFont.begin(drawScale, true, false);
        titleFont.render(brand, cursorX, textY, new RenderColor(titleArgb), false);
        titleFont.end();
        cursorX += brandW;

        if (!meta.isEmpty()) {
            float dotY = textY + textH * 0.5f - DOT_SIZE * drawScale * 0.5f;
            renderer.roundedRect(cursorX + GAP * drawScale * 0.5f, dotY,
                    DOT_SIZE * drawScale, DOT_SIZE * drawScale,
                    DOT_SIZE * drawScale * 0.5f, 1.0f, HudRenderUtil.setAlpha(metaArgb, 0xB0));
            cursorX += dotW;
            metaFont.begin(drawScale, true, false);
            metaFont.render(meta, cursorX, textY, new RenderColor(metaArgb), false);
            metaFont.end();
        }

        width = boxW;
        height = boxH;
    }

    public static String joinMeta(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part == null || part.isBlank()) continue;
            if (!sb.isEmpty()) sb.append("  ");
            sb.append(part);
        }
        return sb.toString();
    }

    private int resolveBg() {
        if (syncTheme.get()) {
            return HudRenderUtil.setAlpha(theme().windowBg(), 0xCC);
        }
        return (0xCC << 24) | (bgColor.getArgb() & 0x00FFFFFF);
    }
}
