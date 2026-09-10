/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.nondraggable.impl.dynamicisland.controls;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.module.lifecycle.Modules;
import dev.loki.lovisual.features.module.modules.misc.ClickGui;
import dev.loki.lovisual.mixins.accessors.hud.BossHealthOverlayAccessor;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import dev.loki.lovisual.util.media.info.RepeatMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.client.gui.screens.Screen;

import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

/** Чистые утилиты Dynamic Island: форматирование, иконки, цвета медиа, замеры, экраны, fallback-рендер. */
public final class IslandUiUtil {

    public static final int BASE_SHADOW = 0xFF000000;
    public static final int BASE_STROKE = 0xFFFFFFFF;

    private IslandUiUtil() {
    }

    public static int shuffleColor(MediaSessionService.Snapshot snapshot) {
        if (snapshot == null || !snapshot.supportsShuffle()) {
            return 0x99000000;
        }
        return snapshot.isShuffleActive() ? 0xFFFFFFFF : 0x7AB7BEC8;
    }

    public static int repeatColor(MediaSessionService.Snapshot snapshot) {
        if (snapshot == null || !snapshot.supportsRepeat()) {
            return 0x99000000;
        }
        RepeatMode mode = snapshot.repeatMode();
        return mode == RepeatMode.ALL || mode == RepeatMode.ONE ? 0xFFFFFFFF : 0x7AB7BEC8;
    }

    public static String repeatSvg(MediaSessionService.Snapshot snapshot) {
        if (snapshot == null || !snapshot.supportsRepeat()) {
            return "repeat-off";
        }
        return snapshot.repeatMode() == RepeatMode.ONE ? "repeat-1" : "repeat";
    }

    public static boolean clickGuiEnabled() {
        ClickGui clickGui = Modules.get(ClickGui.class);
        return clickGui != null && clickGui.isEnabled();
    }

    public static boolean isSettingsLikeScreen(Screen screen) {
        if (screen == null) return false;
        String name = screen.getClass().getName();
        String simple = screen.getClass().getSimpleName();
        if (name.contains(".gui.screen.option.") || name.contains(".gui.screen.pack.")) return true;
        return simple.contains("Options")
                || simple.contains("Option")
                || simple.contains("Controls")
                || simple.contains("Keybind")
                || simple.contains("Video")
                || simple.contains("Sound")
                || simple.contains("Language")
                || simple.contains("Accessibility")
                || simple.contains("Skin")
                || simple.contains("Pack")
                || simple.contains("Datapack")
                || simple.contains("Customize")
                || simple.contains("Telemetry")
                || simple.contains("Credits");
    }

    public static String formatClock(LocalTime time) {
        return String.format("%02d:%02d", time.getHour(), time.getMinute());
    }

    public static String formatSecondsOnly(float seconds) {
        return Math.max(0L, (long) Math.ceil(Math.max(0f, seconds))) + "s";
    }

    public static String formatShortTime(float seconds) {
        return formatShortTime((long) Math.ceil(Math.max(0f, seconds)));
    }

    public static String formatShortTime(long seconds) {
        long sec = Math.max(0L, seconds);
        long m = sec / 60L;
        long s = sec % 60L;
        return String.format("%d:%02d", m, s);
    }

    public static String iconString(int codepoint) {
        return new String(Character.toChars(codepoint));
    }

    public static String hex(int argb) {
        return String.format("#%08X", argb);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public static boolean hit(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public static float measureWidth(TextRenderer renderer, String text, float scale) {
        if (renderer == null || text == null || text.isEmpty()) return 0f;
        renderer.begin(scale, true, false);
        try {
            return (float) renderer.getWidth(text, false);
        } finally {
            renderer.end();
        }
    }

    public static float measureHeight(TextRenderer renderer, float scale) {
        if (renderer == null) return 0f;
        renderer.begin(scale, true, false);
        try {
            return (float) renderer.getHeight(false);
        } finally {
            renderer.end();
        }
    }

    public static float reservedTimeWidth(TextRenderer renderer, String text, float scale) {
        return measureWidth(renderer, reserveTimeTemplate(text), scale);
    }

    public static String reserveTimeTemplate(String text) {
        if (text == null || text.isEmpty()) return "0:00";
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            out.append(Character.isDigit(c) ? '8' : c);
        }
        return out.toString();
    }

    public static void drawFallback(Renderer2D renderer,
                                    float drawX,
                                    float drawY,
                                    float drawWidth,
                                    float drawHeight,
                                    float radius,
                                    float alpha,
                                    int accent,
                                    int fillTop) {
        renderer.roundedRectShadow(drawX, drawY, drawWidth, drawHeight, radius, 8f, 14f,
                HudRenderUtil.scaleAlpha(BASE_SHADOW, 0.28f * alpha));
        renderer.roundedRect(drawX, drawY, drawWidth, drawHeight, radius, 1.1f,
                HudRenderUtil.scaleAlpha(fillTop, 0.88f * alpha));
        renderer.roundedRectStroke(drawX, drawY, drawWidth, drawHeight, radius, 1.1f, 1f,
                HudRenderUtil.scaleAlpha(HudRenderUtil.mixColor(BASE_STROKE, accent, 0.18f), 0.18f * alpha));
    }

    public static float bossBarOffset(Minecraft mc, int logicalScreenH, float topY) {
        if (mc == null || mc.gui == null || mc.getWindow() == null) return 0f;
        BossHealthOverlay bossBarHud = mc.gui.hud.getBossOverlay();
        if (bossBarHud == null) return 0f;
        Map<UUID, LerpingBossEvent> bars;
        try {
            bars = ((BossHealthOverlayAccessor) bossBarHud).getBars();
        } catch (Throwable ignored) {
            return 0f;
        }
        if (bars == null || bars.isEmpty()) return 0f;

        int scaledH = Math.max(1, mc.getWindow().getGuiScaledHeight());
        int y = 12;
        int rows = 0;
        for (LerpingBossEvent ignored : bars.values()) {
            rows++;
            y += 19;
            if (y >= scaledH / 3) break;
        }
        if (rows <= 0) return 0f;

        float bossBottomScaled = 17f + 19f * (rows - 1);
        float hudScale = HudScale.scale(mc.getWindow().getWidth(), mc.getWindow().getHeight());
        float windowScale = (float) mc.getWindow().getGuiScale();
        float bossBottomLogical = (bossBottomScaled + 8f) * windowScale / Math.max(0.001f, hudScale);
        return Math.max(0f, Math.min(logicalScreenH * 0.30f, bossBottomLogical) - topY);
    }
}
