/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.combat;

import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.util.media.info.MediaInfo;
import dev.loki.lovisual.util.media.session.MediaSessionService;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import static dev.loki.lovisual.features.theme.Theme.theme;

final class MediaPlayerRenderHelper {

    private MediaPlayerRenderHelper() {}

    static void render(MediaPlayer p, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        MediaSessionService.Snapshot snapshot = p.mediaService.snapshot();
        p.currentSnapshot = snapshot;
        MediaInfo media = snapshot.media();
        boolean hasSession = snapshot.hasSession();
        boolean mediaAvailable = snapshot.mediaAvailable();

        TextRenderer titleRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, textRenderer);
        TextRenderer artistRenderer = Fonts.renderer("InterMedium", FontInfo.Type.Regular, textRenderer);
        TextRenderer metaRenderer = Fonts.renderer("Inter", FontInfo.Type.Regular, textRenderer);
        TextRenderer iconRenderer = MediaPlayerConstants.getMediaIcons(textRenderer);

        float mediaScale = p.scale.get().floatValue();
        float baseScale = HudScale.scale(screenW, screenH) * mediaScale;
        float fontScale = (p.hud.getFontSize() / 18f) * mediaScale;
        float padding = MediaPlayerConstants.BASE_PADDING * baseScale;
        float gap = MediaPlayerConstants.BASE_GAP * baseScale;
        float artBase = MediaPlayerConstants.BASE_ART_SIZE * baseScale;
        float artBoost = MediaPlayerConstants.BASE_ART_BOOST * baseScale;
        float artOverhang = MediaPlayerConstants.BASE_ART_OVERHANG * baseScale;
        float radius = MediaPlayerConstants.BASE_RADIUS * baseScale;
        float barH = MediaPlayerConstants.BASE_BAR_H * baseScale;
        float textGap = MediaPlayerConstants.BASE_TEXT_GAP * baseScale;
        float barGap = MediaPlayerConstants.BASE_BAR_GAP * baseScale;
        float bottomGap = MediaPlayerConstants.BASE_BOTTOM_GAP * baseScale;
        float buttonGap = MediaPlayerConstants.BASE_BUTTON_GAP * baseScale;
        float contentW = MediaPlayerConstants.BASE_CONTENT_W * baseScale;

        float titleScale = 0.95f * fontScale;
        float artistScale = 0.78f * fontScale;
        float metaScale = 0.70f * fontScale;
        float iconScale = 0.9f * fontScale;

        String title = hasSession ? MediaPlayerConstants.safe(media.title()) : "";
        String artist = hasSession ? MediaPlayerConstants.safe(media.artist()) : "";
        String owner = hasSession && snapshot.session() != null ? MediaPlayerConstants.safe(snapshot.session().getOwner()) : "";
        long predictedPos = hasSession ? snapshot.predictedPositionSeconds() : 0L;
        String timeText = hasSession ? MediaPlayerConstants.formatTime(predictedPos) : "";

        metaRenderer.begin(metaScale, false, false);
        float timeW = hasSession ? (float) metaRenderer.getWidth(timeText, false) : 0f;
        metaRenderer.end();

        boolean showShuffle = hasSession && snapshot.supportsShuffle();
        boolean showRepeat = hasSession && snapshot.supportsRepeat();
        float btnSizeBase = MediaPlayerConstants.BASE_BUTTON_SIZE * baseScale;

        float maxOwnerW = Math.max(0f, contentW - timeW - buttonGap);
        String ownerText = hasSession ? MediaPlayerTextHelper.trimText(metaRenderer, owner, maxOwnerW, metaScale) : "";
        String titleText = hasSession ? title : "";
        String artistText = hasSession ? MediaPlayerTextHelper.trimText(artistRenderer, artist, contentW, artistScale) : "";

        titleRenderer.begin(titleScale, false, false);
        float titleW = hasSession ? (float) titleRenderer.getWidth(titleText, false) : 0f;
        float titleH = (float) titleRenderer.getHeight(false);
        titleRenderer.end();

        artistRenderer.begin(artistScale, false, false);
        if (hasSession) artistRenderer.getWidth(artistText, false);
        float artistH = (float) artistRenderer.getHeight(false);
        artistRenderer.end();

        metaRenderer.begin(metaScale, false, false);
        float metaH = (float) metaRenderer.getHeight(false);
        float ownerW = hasSession ? (float) metaRenderer.getWidth(ownerText, false) : 0f;
        metaRenderer.end();

        float contentH = titleH + textGap + artistH + barGap + barH + bottomGap + metaH;
        float artSize = Math.max(artBase, contentH + artBoost);
        float leftPad = padding + artOverhang;
        float bodyH = Math.max(contentH, artSize);
        float boxW = leftPad + padding + artSize + gap + contentW;
        float boxH = padding * 2f + bodyH;

        p.setSize(boxW, boxH);

        float scale = Math.max(0f, Math.min(1f, p.anim));
        float drawX = p.getX() + (boxW - boxW * scale) * 0.5f;
        float drawY = p.getY() + (boxH - boxH * scale) * 0.5f;

        boolean useSyncTheme = p.syncTheme.get();
        boolean blurEnabled = p.isBlurEffect();
        boolean glassEnabled = p.isGlassEffect();
        int baseRgb = glassEnabled ? HudRenderUtil.glassBackgroundRgb()
                : (useSyncTheme ? (theme().windowBg() & 0x00FFFFFF) : (p.bg.getArgb() & 0x00FFFFFF));
        int bgAlpha = p.getBgAlpha();
        int bg = (bgAlpha << 24) | baseRgb;
        int accent = useSyncTheme ? theme().accent() : (p.accent.getArgb() | 0xFF000000);
        int accentSoft = useSyncTheme ? theme().accentSoft() : HudRenderUtil.mixColor(accent, bg, 0.55f);
        int stroke = HudRenderUtil.scaleAlpha(accent, 0.75f);
        int strokeSoft = HudRenderUtil.scaleAlpha(accentSoft, 0.45f);
        int borderColor = HudRenderUtil.mixColor(stroke, strokeSoft, 0.5f);
        p.uiAccent = accent;
        p.uiAccentSoft = accentSoft;
        p.uiText = useSyncTheme ? theme().textPrimary() : (p.text.getArgb() | 0xFF000000);
        p.uiTextMuted = useSyncTheme ? theme().textMuted() : HudRenderUtil.mixColor(p.uiText, bg, 0.6f);

        if (glassEnabled) {
            MediaPlayerTextHelper.drawGlass(p, drawX, drawY, boxW * scale, boxH * scale, radius * scale, baseScale * scale, p.uiAccentSoft);
        } else if (blurEnabled) {
            MediaPlayerTextHelper.drawBlur(p, drawX, drawY, boxW * scale, boxH * scale, radius * scale, bg);
        }
        int bgScaled = glassEnabled ? HudRenderUtil.glassPanelBackground(scale) : HudRenderUtil.scaleAlpha(bg, scale);
        boolean useGradient = !glassEnabled && useSyncTheme && p.gradient.get();
        HudRenderUtil.drawHudBackground(renderer, drawX, drawY, boxW * scale, boxH * scale, radius * scale, 1.0f, bgScaled, useGradient);
        if (!glassEnabled) {
            renderer.roundedRectStroke(drawX, drawY, boxW * scale, boxH * scale, radius * scale, 1.0f, 0.9f * baseScale * scale, HudRenderUtil.scaleAlpha(borderColor, scale));
        }

        float contentY = padding + (bodyH - contentH) * 0.5f;
        if (!hasSession) {
            MediaPlayerTextHelper.drawNoSession(p, renderer, metaRenderer, drawX, drawY, boxW, boxH, scale, metaScale, mediaAvailable);
            return;
        }

        float artX = leftPad - (2f * baseScale);
        float artY = padding + (bodyH - artSize) * 0.5f;
        float textX = leftPad + artSize + gap;
        float artistY = contentY + titleH + textGap;
        float barY = artistY + artistH + barGap;
        float bottomY = barY + barH + bottomGap;

        MediaPlayerTextHelper.drawArtwork(p, renderer, snapshot, drawX, drawY, artX, artY, artSize, radius, baseScale, scale);
        MediaPlayerTextHelper.drawTexts(p, renderer, titleRenderer, artistRenderer, metaRenderer, iconRenderer, titleText, artistText, timeText, ownerText, ownerW,
                drawX, drawY, textX, contentY, artistY, barY, bottomY, contentW, titleW, titleH, titleScale, artistScale, metaScale, iconScale,
                barH, baseScale, scale, snapshot, btnSizeBase, buttonGap, metaH, showShuffle, showRepeat);
    }
}
