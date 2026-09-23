/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.hud.draggable.impl.panels;

import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;

import static dev.loki.lovisual.features.theme.Theme.theme;

final class ScoreboardRenderHelper {

    private ScoreboardRenderHelper() {}

    static void updatePalette(Scoreboard board) {
        if (board.isThemeMode()) {
            int panelAlpha = board.bgAlpha.get();
            board.uiBgPrimary = HudRenderUtil.setAlpha(theme().windowBg(), panelAlpha);
            board.uiBgSecondary = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().surface(), theme().windowHeader(), 0.35f), panelAlpha);
            board.uiHeaderBg = HudRenderUtil.setAlpha(theme().windowHeader(), panelAlpha);
            board.uiStroke = HudRenderUtil.setAlpha(
                    HudRenderUtil.mixColor(theme().windowStroke(), theme().strokeSoft(), 0.4f),
                    Math.min(panelAlpha, 190));
            board.uiTitleColor = theme().textPrimary();
            board.uiLabelColor = theme().textMuted();
            board.uiValueColor = theme().textPrimary();
            board.uiFooterColor = HudRenderUtil.mixColor(theme().accent(), theme().textPrimary(), 0.35f);
            return;
        }
        board.uiBgPrimary = board.bg.getArgb();
        board.uiBgSecondary = board.bg2.getArgb();
        board.uiHeaderBg = HudRenderUtil.mixColor(board.bg2.getArgb(), board.bg.getArgb(), 0.2f);
        board.uiStroke = board.stroke.getArgb();
        board.uiTitleColor = board.titleColor.getArgb();
        board.uiLabelColor = board.labelColor.getArgb();
        board.uiValueColor = board.valueColor.getArgb();
        board.uiFooterColor = board.footerColor.getArgb();
    }

    static void drawBlur(Scoreboard board, float x, float y, float w, float h, float radius) {
        if (!board.hasEffect()) return;
        float quality = board.hud.getBlurRadius();
        float alpha = board.blurAlpha.get() / 255f;
        Renderer2D.COLOR.blurRect(x, y, w, h, radius, quality, 1.0f, alpha, 0xFFFFFF);
    }

    static void drawBackground(Renderer2D renderer, Scoreboard board, float x, float y, float w, float h, float radius, float s) {
        if (board.isThemeMode()) {
            HudRenderUtil.drawHudBackground(renderer, x, y, w, h, radius, Scoreboard.BASE_SOFTNESS, board.uiBgPrimary, true);
        } else {
            renderer.roundedRectGradientQuad(x, y, w, h, radius, Scoreboard.BASE_SOFTNESS, board.uiBgPrimary, board.uiBgSecondary, board.uiBgPrimary, board.uiBgSecondary);
        }
        renderer.roundedRectStroke(x, y, w, h, radius, Scoreboard.BASE_SOFTNESS, Math.max(0.5f, Scoreboard.BASE_STROKE * s), board.uiStroke);
    }

    static void drawHeader(Renderer2D renderer, Scoreboard board, float x, float y, float w, float hh, float radius) {
        renderer.roundedRectCorners(x, y, w, hh, radius, radius, 0.0f, 0.0f, Scoreboard.BASE_SOFTNESS, board.uiHeaderBg);
    }

    static void render(Scoreboard board, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx, float tickDelta, int screenW, int screenH) {
        Minecraft mc = Minecraft.getInstance();
        boolean preview = DraggableHudElementRegistry.isForceVisible();
        if (mc == null || (mc.level == null && !preview)) { board.setSize(0.0f, 0.0f); return; }
        if (!preview && Scoreboard.isHiddenByNoRender()) { board.setSize(0.0f, 0.0f); return; }
        if (!preview && !board.isEnabled()) { board.setSize(0.0f, 0.0f); return; }
        Objective objective = ScoreboardDataHelper.resolveObjective();
        if (objective == null && !preview) { board.setSize(0.0f, 0.0f); return; }
        updatePalette(board);
        Component title = ScoreboardDataHelper.resolveTitle(objective, preview);
        board.lines.clear();
        board.lines.addAll(ScoreboardDataHelper.resolveLines(objective, preview));
        if (board.lines.isEmpty()) { board.setSize(0.0f, 0.0f); return; }
        float baseScale = HudScale.scale(screenW, screenH) * board.scale.get().floatValue();
        float padX = Scoreboard.BASE_PAD_X * baseScale, padY = Scoreboard.BASE_PAD_Y * baseScale;
        float rowGap = Scoreboard.BASE_ROW_GAP * baseScale, scoreGap = Scoreboard.BASE_SCORE_GAP * baseScale;
        float footerGap = Scoreboard.BASE_FOOTER_GAP * baseScale, radius = Scoreboard.BASE_RADIUS * baseScale;
        float titleScale = Scoreboard.BASE_TITLE_SCALE * baseScale, textScale = Scoreboard.BASE_TEXT_SCALE * baseScale;
        float time = (float) (net.minecraft.util.Util.getMillis() / 1000.0);
        HudTextEffects.Effect effect = board.textEffect.get();
        int effectSpeed = board.textEffectSpeed.get();
        TextRenderer fallback = textRenderer != null ? textRenderer : TextRenderer.get();
        TextRenderer titleRenderer = Fonts.renderer("Iosevka", FontInfo.Type.BoldItalic, fallback);
        TextRenderer bodyRenderer = Fonts.renderer("Iosevka", FontInfo.Type.Regular, titleRenderer);
        Component normalizedTitle = ScoreboardDataHelper.normalizeHeaderTitle(title);
        var titleLines = ScoreboardTextHelper.splitStyledLines(normalizedTitle, board.uiTitleColor);
        float titleW = ScoreboardTextHelper.measureStyledLineWidth(titleRenderer, titleLines, titleScale);
        float titleH = ScoreboardTextHelper.rendererHeight(titleRenderer, titleScale);
        float textH = ScoreboardTextHelper.rendererHeight(bodyRenderer, textScale);
        float maxLabelW = 0.0f, maxValueW = 0.0f, maxFooterW = 0.0f;
        for (Scoreboard.SidebarLine line : board.lines) {
            if (line.footer()) maxFooterW = Math.max(maxFooterW, ScoreboardTextHelper.measureStyledText(bodyRenderer, line.label(), board.uiFooterColor, textScale));
            else {
                maxLabelW = Math.max(maxLabelW, ScoreboardTextHelper.measureStyledText(bodyRenderer, line.label(), board.uiLabelColor, textScale));
                if (!ScoreboardDataHelper.isBlankText(line.value())) maxValueW = Math.max(maxValueW, ScoreboardTextHelper.measureStyledText(bodyRenderer, line.value(), board.uiValueColor, textScale));
            }
        }
        board.displayLabelWidth = Scoreboard.smoothWidth(board.displayLabelWidth, maxLabelW);
        board.displayValueWidth = Scoreboard.smoothWidth(board.displayValueWidth, maxValueW);
        board.displayFooterWidth = Scoreboard.smoothWidth(board.displayFooterWidth, maxFooterW);
        float widestRow = board.displayLabelWidth + (board.displayValueWidth > 0.0f ? scoreGap + board.displayValueWidth : 0.0f);
        float widest = Math.max(titleW, Math.max(widestRow, board.displayFooterWidth));
        float width = padX * 2.0f + widest;
        float titleLineGap = Scoreboard.BASE_TITLE_LINE_GAP * baseScale;
        float titleBlockHeight = titleLines.size() * titleH + Math.max(0, titleLines.size() - 1) * titleLineGap;
        float headerHeight = titleBlockHeight + padY * 2.0f;
        int footerCount = 0, bodyCount = 0;
        for (Scoreboard.SidebarLine line : board.lines) if (line.footer()) footerCount++; else bodyCount++;
        float rowsH = bodyCount > 0 ? bodyCount * textH + Math.max(0, bodyCount - 1) * rowGap : 0.0f;
        float footersH = footerCount > 0 ? footerCount * textH + Math.max(0, footerCount - 1) * rowGap : 0.0f;
        float footerSectionGap = footerCount > 0 && bodyCount > 0 ? footerGap : 0.0f;
        float height = headerHeight + padY + rowsH + footerSectionGap + footersH + padY;
        board.setSize(width, height);
        float x = board.getX();
        float y = board.getY();
        if (board.isBlurEffect()) drawBlur(board, x, y, width, height, radius);
        drawBackground(renderer, board, x, y, width, height, radius, baseScale);
        drawHeader(renderer, board, x, y, width, headerHeight, radius);
        float cursorY = y + headerHeight + padY;
        float titleY = y + (headerHeight - titleBlockHeight) * 0.5f;
        ScoreboardTextRenderer.renderStyledLinesCentered(renderer, titleRenderer, titleScale, x, width, titleY, titleLines, titleLineGap, board.textEffectSections.get("header"), effect, effectSpeed, time);
        boolean footerStarted = false;
        for (Scoreboard.SidebarLine line : board.lines) {
            if (line.footer()) {
                if (!footerStarted && bodyCount > 0) { cursorY += footerGap; footerStarted = true; }
                ScoreboardTextRenderer.renderStyledText(renderer, bodyRenderer, textScale, x + padX, cursorY, line.label(), board.uiFooterColor, board.textEffectSections.get("footer"), effect, effectSpeed, time, 0.0f);
                cursorY += textH + rowGap; continue;
            }
            ScoreboardTextRenderer.renderStyledText(renderer, bodyRenderer, textScale, x + padX, cursorY, line.label(), board.uiLabelColor, board.textEffectSections.get("labels"), effect, effectSpeed, time, 0.0f);
            if (!ScoreboardDataHelper.isBlankText(line.value())) {
                float valueW = ScoreboardTextHelper.measureStyledText(bodyRenderer, line.value(), board.uiValueColor, textScale);
                float valueX = x + width - padX - valueW;
                ScoreboardTextRenderer.renderStyledText(renderer, bodyRenderer, textScale, valueX, cursorY, line.value(), board.uiValueColor, board.textEffectSections.get("values"), effect, effectSpeed, time, 0.2f);
            }
            cursorY += textH + rowGap;
        }
    }
}
