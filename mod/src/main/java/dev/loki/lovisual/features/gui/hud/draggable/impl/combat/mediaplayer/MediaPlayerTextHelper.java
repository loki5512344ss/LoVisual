package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

final class MediaPlayerTextHelper {

    private MediaPlayerTextHelper() {}

    static void drawScrollingTitle(MediaPlayer p, TextRenderer renderer, String text, float x, float y, float fullWidth, float viewWidth, float scale, float baseScale, float alpha) {
        float speed = MediaPlayerConstants.BASE_TITLE_SCROLL_SPEED * baseScale;
        float gap = MediaPlayerConstants.BASE_TITLE_SCROLL_GAP * baseScale;
        if (speed <= 0f) return;
        float cycleDistance = fullWidth + gap;
        float cycleTime = MediaPlayerConstants.TITLE_SCROLL_PAUSE_SEC + (cycleDistance / speed);
        if (cycleTime <= 0f) return;
        float now = Util.getMillis() / 1000f;
        float t = now % cycleTime;
        float offset = 0f;
        if (t > MediaPlayerConstants.TITLE_SCROLL_PAUSE_SEC) {
            offset = -(t - MediaPlayerConstants.TITLE_SCROLL_PAUSE_SEC) * speed;
        }
        int color = HudRenderUtil.scaleAlpha(p.uiText, alpha);
        RenderColor renderColor = new RenderColor(color);
        float fade = Math.min(viewWidth * 0.25f, MediaPlayerConstants.BASE_TITLE_FADE * baseScale);
        renderer.begin(scale, false, false);
        try {
            renderer.renderHorizontalFadeClipped(text, x + offset, y, renderColor, x, x + viewWidth, fade, fade, false);
            if (cycleDistance > viewWidth * 0.5f) {
                renderer.renderHorizontalFadeClipped(text, x + offset + cycleDistance, y, renderColor, x, x + viewWidth, fade, fade, false);
            }
        } finally {
            renderer.end();
        }
    }

    static float getTextHeight(TextRenderer renderer, float scale) {
        renderer.begin(scale, false, false);
        try {
            return (float) renderer.getHeight(false);
        } finally {
            renderer.end();
        }
    }

    static String trimText(TextRenderer tr, String text, float maxWidth, float scale) {
        if (text == null || text.isEmpty()) return "";
        tr.begin(scale, false, false);
        try {
            if (tr.getWidth(text, false) <= maxWidth) return text;
            String value = text;
            while (!value.isEmpty()) {
                String candidate = value + "...";
                if (tr.getWidth(candidate, false) <= maxWidth) return candidate;
                value = value.substring(0, value.length() - 1);
            }
            return "";
        } finally {
            tr.end();
        }
    }

    static void drawNoSession(MediaPlayer p, Renderer2D renderer, TextRenderer metaRenderer, float drawX, float drawY, float boxW, float boxH, float scale, float metaScale, boolean mediaAvailable) {
        String key = mediaAvailable ? "hud.media_player.available" : "hud.media_player.unavailable";
        String status = I18n.get(key);
        metaRenderer.begin(metaScale * scale, false, false);
        float statusW = (float) metaRenderer.getWidth(status, false);
        float statusH = (float) metaRenderer.getHeight(false);
        metaRenderer.end();
        float sx = drawX + (boxW * scale - statusW) * 0.5f;
        float sy = drawY + (boxH * scale - statusH) * 0.5f;
        metaRenderer.begin(metaScale * scale, false, false);
        metaRenderer.render(status, sx, sy, new RenderColor(HudRenderUtil.scaleAlpha(p.uiTextMuted, scale)), false);
        metaRenderer.end();
        MediaPlayerControls.updateButtons(p);
    }

    static void drawArtwork(MediaPlayer p, Renderer2D renderer, MediaSessionService.Snapshot snapshot, float drawX, float drawY, float artX, float artY, float artSize, float radius, float baseScale, float scale) {
        float drawArtX = drawX + artX * scale;
        float drawArtY = drawY + artY * scale;
        float drawArtSize = artSize * scale;
        float artRadius = Math.min(radius * 1.2f, artSize * 0.22f) * scale;
        Identifier tex = snapshot.artworkTexture();
        if (tex != null) {
            Renderer2D.TEXTURE.roundedTexRect(drawArtX, drawArtY, drawArtSize, drawArtSize, artRadius, 1.0f, HudRenderUtil.scaleAlpha(0xFFFFFFFF, scale), tex);
        } else {
            renderer.roundedRect(drawArtX, drawArtY, drawArtSize, drawArtSize, artRadius, 1.0f, HudRenderUtil.scaleAlpha(0x44111111, scale));
            renderer.roundedRectStroke(drawArtX, drawArtY, drawArtSize, drawArtSize, artRadius, 1.0f, 0.9f * baseScale * scale, HudRenderUtil.scaleAlpha(p.uiAccentSoft, scale * 0.9f));
        }
    }

    static void drawBlur(MediaPlayer p, float x, float y, float w, float h, float radius, int tintRgb) {
        float quality = p.hud.getBlurRadius();
        float alpha = p.blurAlpha.get() / 255f;
        Renderer2D.COLOR.blurRect(x, y, w, h, radius, quality, 1.0f, alpha, 0xFFFFFF);
    }

    static void drawGlass(MediaPlayer p, float x, float y, float w, float h, float radius, float scale, int accentSoft) {
        float alpha = p.blurAlpha.get() / 255f;
        HudRenderUtil.drawLiquidGlass(x, y, w, h, radius, scale, true, alpha);
    }

    static void drawTexts(MediaPlayer p, Renderer2D renderer, TextRenderer titleRenderer, TextRenderer artistRenderer, TextRenderer metaRenderer, TextRenderer iconRenderer,
                          String titleText, String artistText, String timeText, String ownerText, float ownerW,
                          float drawX, float drawY, float textX, float contentY, float artistY, float barY, float bottomY,
                          float contentW, float titleW, float titleH, float titleScale, float artistScale, float metaScale, float iconScale,
                          float barH, float baseScale, float scale, MediaSessionService.Snapshot snapshot, float btnSizeBase, float buttonGap, float metaH, boolean showShuffle, boolean showRepeat) {
        float titleRenderX = drawX + textX * scale;
        float titleRenderY = drawY + contentY * scale;
        float titleRenderW = contentW * scale;
        boolean titleScroll = titleW > contentW * 1.01f;
        if (titleScroll) {
            drawScrollingTitle(p, titleRenderer, titleText, titleRenderX, titleRenderY, titleW * scale, titleRenderW, titleScale * scale, baseScale, scale);
        } else {
            titleRenderer.begin(titleScale * scale, false, false);
            titleRenderer.render(titleText, titleRenderX, titleRenderY, new RenderColor(HudRenderUtil.scaleAlpha(p.uiText, scale)), false);
            titleRenderer.end();
        }
        artistRenderer.begin(artistScale * scale, false, false);
        artistRenderer.render(artistText, drawX + textX * scale, drawY + artistY * scale, new RenderColor(HudRenderUtil.scaleAlpha(p.uiTextMuted, scale)), false);
        artistRenderer.end();
        float barX = drawX + textX * scale;
        float barW = contentW * scale;
        float barRadius = Math.max(1f, barH * 0.5f) * scale;
        int barBg = HudRenderUtil.scaleAlpha(0x55000000, scale);
        renderer.roundedRect(barX, drawY + barY * scale, barW, barH * scale, barRadius, 1.0f, barBg);
        long predictedPos = snapshot.predictedPositionSeconds();
        long predictedDur = snapshot.durationSeconds();
        float progress = predictedDur > 0 ? Math.min(1.0f, Math.max(0.0f, (float) predictedPos / (float) predictedDur)) : 0f;
        int barFill = HudRenderUtil.scaleAlpha(p.uiAccent, scale);
        renderer.roundedRect(barX, drawY + barY * scale, barW * progress, barH * scale, barRadius, 1.0f, barFill);
        metaRenderer.begin(metaScale * scale, false, false);
        metaRenderer.render(timeText, drawX + textX * scale, drawY + bottomY * scale, new RenderColor(HudRenderUtil.scaleAlpha(p.uiTextMuted, scale)), false);
        float ownerX = drawX + (textX + contentW - ownerW) * scale;
        metaRenderer.render(ownerText, ownerX, drawY + bottomY * scale, new RenderColor(HudRenderUtil.scaleAlpha(p.uiTextMuted, scale)), false);
        metaRenderer.end();
        boolean chatOpen = net.minecraft.client.Minecraft.getInstance() != null && dev.loki.lovisual.util.screen.ClientScreen.current() instanceof net.minecraft.client.gui.screens.ChatScreen;
        MediaPlayerControls.layoutControls(p, drawX, drawY, textX, contentW, bottomY, metaH, btnSizeBase, buttonGap, baseScale, showShuffle, showRepeat, scale);
        float controlIconScale = iconScale * 1.05f;
        MediaPlayerControls.updateHover(p, chatOpen, showShuffle, showRepeat);
        if (showShuffle) {
            MediaPlayerControls.drawControl(renderer, iconRenderer, MediaPlayerConstants.iconString(MediaPlayerConstants.ICON_SHUFFLE), p.shuffleX, p.shuffleY, p.buttonSize, p.shuffleHover, scale, controlIconScale, MediaPlayerControls.shuffleColor(snapshot, scale), p);
        }
        MediaPlayerControls.drawControl(renderer, iconRenderer, MediaPlayerConstants.iconString(MediaPlayerConstants.ICON_PREV), p.prevX, p.prevY, p.buttonSize, p.prevHover, scale, controlIconScale, HudRenderUtil.scaleAlpha(p.uiText, scale), p);
        MediaPlayerControls.drawControl(renderer, iconRenderer, MediaPlayerConstants.iconString(snapshot.isPlaying() ? MediaPlayerConstants.ICON_PAUSE : MediaPlayerConstants.ICON_PLAY), p.playX, p.playY, p.buttonSize, p.playHover, scale, controlIconScale, HudRenderUtil.scaleAlpha(p.uiText, scale), p);
        MediaPlayerControls.drawControl(renderer, iconRenderer, MediaPlayerConstants.iconString(MediaPlayerConstants.ICON_NEXT), p.nextX, p.nextY, p.buttonSize, p.nextHover, scale, controlIconScale, HudRenderUtil.scaleAlpha(p.uiText, scale), p);
        if (showRepeat) {
            MediaPlayerControls.drawRepeatControl(renderer, snapshot, p.repeatX, p.repeatY, p.buttonSize, p.repeatHover, scale, p);
        }
    }
}
