package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.util.media.info.RepeatMode;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import net.minecraft.client.Minecraft;

final class MediaPlayerControls {

    private MediaPlayerControls() {}

    static boolean isMouseOverInteractive(MediaPlayer p, float mx, float my) {
        if (p.buttonSize <= 0f) return false;
        boolean showShuffle = p.currentSnapshot != null && p.currentSnapshot.supportsShuffle();
        boolean showRepeat = p.currentSnapshot != null && p.currentSnapshot.supportsRepeat();
        return MediaPlayerConstants.hit(mx, my, p.playX, p.playY, p.buttonSize, p.buttonSize)
                || (showShuffle && MediaPlayerConstants.hit(mx, my, p.shuffleX, p.shuffleY, p.buttonSize, p.buttonSize))
                || MediaPlayerConstants.hit(mx, my, p.prevX, p.prevY, p.buttonSize, p.buttonSize)
                || MediaPlayerConstants.hit(mx, my, p.nextX, p.nextY, p.buttonSize, p.buttonSize)
                || (showRepeat && MediaPlayerConstants.hit(mx, my, p.repeatX, p.repeatY, p.buttonSize, p.buttonSize));
    }

    static boolean handleClick(MediaPlayer p, float mx, float my, int button) {
        if (p.currentSnapshot == null || !p.currentSnapshot.hasSession()) return false;
        if (button != 0) return false;
        try {
            if (p.currentSnapshot.supportsShuffle() && MediaPlayerConstants.hit(mx, my, p.shuffleX, p.shuffleY, p.buttonSize, p.buttonSize)) {
                p.mediaService.toggleShuffle();
                return true;
            }
            if (MediaPlayerConstants.hit(mx, my, p.playX, p.playY, p.buttonSize, p.buttonSize)) {
                p.mediaService.playPause();
                return true;
            }
            if (MediaPlayerConstants.hit(mx, my, p.prevX, p.prevY, p.buttonSize, p.buttonSize)) {
                p.mediaService.previous();
                return true;
            }
            if (MediaPlayerConstants.hit(mx, my, p.nextX, p.nextY, p.buttonSize, p.buttonSize)) {
                p.mediaService.next();
                return true;
            }
            if (p.currentSnapshot.supportsRepeat() && MediaPlayerConstants.hit(mx, my, p.repeatX, p.repeatY, p.buttonSize, p.buttonSize)) {
                p.mediaService.cycleRepeatMode();
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    static void updateButtons(MediaPlayer p) {
        p.shuffleX = 0f;
        p.shuffleY = 0f;
        p.playX = 0f;
        p.playY = 0f;
        p.prevX = 0f;
        p.prevY = 0f;
        p.nextX = 0f;
        p.nextY = 0f;
        p.repeatX = 0f;
        p.repeatY = 0f;
        p.buttonSize = 0f;
        p.playHover = 0f;
        p.shuffleHover = 0f;
        p.prevHover = 0f;
        p.nextHover = 0f;
        p.repeatHover = 0f;
    }

    static void updateHover(MediaPlayer p, boolean chatOpen, boolean showShuffle, boolean showRepeat) {
        if (!chatOpen) {
            p.playHover = MediaPlayerConstants.lerp(p.playHover, 0f, 0.2f);
            p.shuffleHover = MediaPlayerConstants.lerp(p.shuffleHover, 0f, 0.2f);
            p.prevHover = MediaPlayerConstants.lerp(p.prevHover, 0f, 0.2f);
            p.nextHover = MediaPlayerConstants.lerp(p.nextHover, 0f, 0.2f);
            p.repeatHover = MediaPlayerConstants.lerp(p.repeatHover, 0f, 0.2f);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        int fbw = mc.getWindow().getWidth();
        int fbh = mc.getWindow().getHeight();
        float uiScale = HudScale.scale(fbw, fbh);
        float mx = HudScale.toVirtual((float) mc.mouseHandler.xpos(), uiScale);
        float my = HudScale.toVirtual((float) mc.mouseHandler.ypos(), uiScale);
        p.shuffleHover = MediaPlayerConstants.lerp(p.shuffleHover, showShuffle && MediaPlayerConstants.hit(mx, my, p.shuffleX, p.shuffleY, p.buttonSize, p.buttonSize) ? 1f : 0f, 0.25f);
        p.playHover = MediaPlayerConstants.lerp(p.playHover, MediaPlayerConstants.hit(mx, my, p.playX, p.playY, p.buttonSize, p.buttonSize) ? 1f : 0f, 0.25f);
        p.prevHover = MediaPlayerConstants.lerp(p.prevHover, MediaPlayerConstants.hit(mx, my, p.prevX, p.prevY, p.buttonSize, p.buttonSize) ? 1f : 0f, 0.25f);
        p.nextHover = MediaPlayerConstants.lerp(p.nextHover, MediaPlayerConstants.hit(mx, my, p.nextX, p.nextY, p.buttonSize, p.buttonSize) ? 1f : 0f, 0.25f);
        p.repeatHover = MediaPlayerConstants.lerp(p.repeatHover, showRepeat && MediaPlayerConstants.hit(mx, my, p.repeatX, p.repeatY, p.buttonSize, p.buttonSize) ? 1f : 0f, 0.25f);
    }

    static void drawControl(Renderer2D renderer, TextRenderer iconRenderer, String icon, float x, float y, float size, float hover, float alpha, float iconScale, int iconColor, MediaPlayer p) {
        if (size <= 0f) return;
        float radius = size * 0.5f;
        float hoverEase = MediaPlayerConstants.clamp01(hover);
        int bg = HudRenderUtil.scaleAlpha(p.uiAccentSoft, hoverEase * 0.45f * alpha);
        if (hoverEase > 0.01f) {
            renderer.roundedRect(x, y, size, size, radius, 1.0f, bg);
        }
        iconRenderer.begin(iconScale * alpha, false, false);
        float iw = (float) iconRenderer.getWidth(icon, false);
        float ih = (float) iconRenderer.getHeight(false);
        float ix = x + (size - iw) * 0.5f;
        float iy = y + (size - ih) * 0.5f;
        iconRenderer.render(icon, ix, iy, new RenderColor(iconColor), false);
        iconRenderer.end();
    }

    static void drawRepeatControl(Renderer2D renderer, MediaSessionService.Snapshot snapshot, float x, float y, float size, float hover, float alpha, MediaPlayer p) {
        if (size <= 0f) return;
        float radius = size * 0.5f;
        float hoverEase = MediaPlayerConstants.clamp01(hover);
        int bg = HudRenderUtil.scaleAlpha(p.uiAccentSoft, hoverEase * 0.45f * alpha);
        if (hoverEase > 0.01f) {
            renderer.roundedRect(x, y, size, size, radius, 1.0f, bg);
        }
        String icon = repeatSvg(snapshot);
        int color = repeatColor(snapshot, alpha);
        float iconSize = size * 0.78f;
        float ix = x + (size - iconSize) * 0.5f;
        float iy = y + (size - iconSize) * 0.5f;
        renderer.svg(icon, ix, iy, iconSize, iconSize, SvgRenderOptions.overrideColor(color));
    }

    static int shuffleColor(MediaSessionService.Snapshot snapshot, float alpha) {
        if (snapshot == null || !snapshot.supportsShuffle()) {
            return HudRenderUtil.scaleAlpha(0xFF000000, 0.36f * alpha);
        }
        return snapshot.isShuffleActive()
                ? HudRenderUtil.scaleAlpha(0xFFFFFFFF, alpha)
                : HudRenderUtil.scaleAlpha(0xFFB7BEC8, 0.48f * alpha);
    }

    static int repeatColor(MediaSessionService.Snapshot snapshot, float alpha) {
        if (snapshot == null || !snapshot.supportsRepeat()) {
            return HudRenderUtil.scaleAlpha(0xFF000000, 0.36f * alpha);
        }
        RepeatMode mode = snapshot.repeatMode();
        return mode == RepeatMode.ALL || mode == RepeatMode.ONE
                ? HudRenderUtil.scaleAlpha(0xFFFFFFFF, alpha)
                : HudRenderUtil.scaleAlpha(0xFFB7BEC8, 0.48f * alpha);
    }

    static String repeatSvg(MediaSessionService.Snapshot snapshot) {
        if (snapshot == null || !snapshot.supportsRepeat()) {
            return "repeat-off";
        }
        return snapshot.repeatMode() == RepeatMode.ONE ? "repeat-1" : "repeat";
    }

    static void layoutControls(MediaPlayer p, float drawX, float drawY, float textX, float contentW, float bottomY, float metaH, float btnSizeBase, float buttonGap, float baseScale, boolean showShuffle, boolean showRepeat, float scale) {
        float active = 3 + (showShuffle ? 1 : 0) + (showRepeat ? 1 : 0);
        float reserved = btnSizeBase * active + buttonGap * Math.max(0, active - 1);
        float controlsX = textX + (contentW - reserved) * 0.5f;
        float controlsY = bottomY + (metaH - btnSizeBase) * 0.5f;
        float cursorX = controlsX;
        if (showShuffle) {
            p.shuffleX = drawX + cursorX * scale;
            p.shuffleY = drawY + controlsY * scale;
            cursorX += btnSizeBase + buttonGap;
        } else {
            p.shuffleX = 0f;
            p.shuffleY = 0f;
        }
        p.prevX = drawX + cursorX * scale;
        p.prevY = drawY + controlsY * scale;
        cursorX += btnSizeBase + buttonGap;
        p.playX = drawX + cursorX * scale;
        p.playY = p.prevY;
        cursorX += btnSizeBase + buttonGap;
        p.nextX = drawX + cursorX * scale;
        p.nextY = p.prevY;
        cursorX += btnSizeBase + buttonGap;
        if (showRepeat) {
            p.repeatX = drawX + cursorX * scale;
            p.repeatY = p.prevY;
        } else {
            p.repeatX = 0f;
            p.repeatY = 0f;
        }
        p.buttonSize = btnSizeBase * scale;
    }
}
