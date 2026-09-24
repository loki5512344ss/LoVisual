package dev.loki.lovisual.features.gui.hud.draggable.impl.status;

import dev.loki.lovisual.features.gui.hud.base.HudTextEffects;
import dev.loki.lovisual.features.gui.hud.config.HudRenderUtil;
import dev.loki.lovisual.features.gui.hud.draggable.impl.HudPanelLayoutModes;
import dev.loki.lovisual.features.gui.hud.draggable.impl.scripted.ScriptedListHudPanel;
import dev.loki.lovisual.features.relations.StaffData;
import dev.loki.lovisual.features.relations.StaffTracker;
import dev.loki.lovisual.render.engine.math.HudScale;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

import java.util.LinkedHashMap;
import java.util.List;

final class AdminsRenderer {

    private AdminsRenderer() {}

    static void render(Admins host, Renderer2D renderer, TextRenderer textRenderer, GuiGraphicsExtractor ctx,
                       float tickDelta, int screenW, int screenH) {
        boolean forceVisible = dev.loki.lovisual.features.gui.hud.draggable.DraggableHudElementRegistry.isForceVisible();
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc == null || (mc.player == null && !forceVisible)) { host.setEmpty(); return; }
        boolean chatPreview = dev.loki.lovisual.util.screen.ClientScreen.current() instanceof net.minecraft.client.gui.screens.ChatScreen;
        List<StaffData.StaffEntry> entries = AdminsData.filterStaff(StaffData.INSTANCE.getEntries());
        List<Admins.AnimatedEntry> animatedEntries = AdminsAnimator.animateEntries(host, entries);
        boolean showExampleRow = entries.isEmpty() && (forceVisible || chatPreview);
        boolean showWidget = !entries.isEmpty() || showExampleRow;
        host.visibilityAnim = HudRenderUtil.animateVisibility(host.visibilityAnim, showWidget);
        float widgetScale = HudRenderUtil.visibilityScale(host.visibilityAnim);
        if (animatedEntries.isEmpty() && !showExampleRow && host.visibilityAnim <= 0.0f) { host.setEmpty(); return; }
        AdminsPalette.updatePalette(host);
        TextRenderer headerIconRenderer = Fonts.renderer("IconsNur", FontInfo.Type.Regular, TextRenderer.get());
        TextRenderer headerTextRenderer = Fonts.renderer("Inter", FontInfo.Type.Bold, TextRenderer.get());
        TextRenderer rowTextRenderer = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, TextRenderer.get());
        if (headerTextRenderer == null) headerTextRenderer = textRenderer;
        if (rowTextRenderer == null) rowTextRenderer = textRenderer;
        if (headerIconRenderer == null) headerIconRenderer = textRenderer;
        float baseScale = HudScale.scale(screenW, screenH) * 1.1f * HudPanelLayoutModes.effectiveScale(host.scaleValue);
        float rowStep = Admins.ROW_STEP * baseScale;
        float fontScale = 0.98f * (host.hud.getFontSize() / 18.0f);
        headerIconRenderer.begin(fontScale, true, false);
        float headerIconH = (float) headerIconRenderer.getHeight(false);
        headerIconRenderer.end();
        headerTextRenderer.begin(fontScale, true, false);
        float headerTextH = (float) headerTextRenderer.getHeight(false);
        float titleWidth = (float) headerTextRenderer.getWidth("Admins", false);
        headerTextRenderer.end();
        long now = System.currentTimeMillis();
        rowTextRenderer.begin(fontScale, true, false);
        float rowTextH = (float) rowTextRenderer.getHeight(false);
        float maxWidth = Admins.MIN_WIDTH * baseScale;
        if (showExampleRow) {
            float previewNameWidth = (float) rowTextRenderer.getWidth("Example Staff", false);
            float previewStatusWidth = (float) rowTextRenderer.getWidth("Vanish", false);
            maxWidth = Math.max(maxWidth, previewNameWidth + previewStatusWidth + (30.0f * baseScale));
        } else {
            for (Admins.AnimatedEntry ae : animatedEntries) {
                float nameWidth = AdminsData.measureLegacyName(rowTextRenderer, ae.entry().displayName());
                float statusWidth = (float) rowTextRenderer.getWidth(AdminsData.formatStatus(ae.entry(), now), false);
                maxWidth = Math.max(maxWidth, nameWidth + statusWidth + (30.0f * baseScale));
            }
        }
        rowTextRenderer.end();
        rowTextRenderer.begin(fontScale * 0.92f, false, false);
        String activeCountText = Integer.toString(entries.size());
        float activeLabelWidth = (float) rowTextRenderer.getWidth("Active:", false);
        float activeCountWidth = (float) rowTextRenderer.getWidth(activeCountText, false);
        rowTextRenderer.end();
        float headerWidth = (Admins.TITLE_TEXT_X * baseScale) + titleWidth + activeLabelWidth + activeCountWidth + ((Admins.COUNT_LABEL_OFFSET + 12.0f) * baseScale);
        maxWidth = Math.max(maxWidth, headerWidth);
        float contentHeight = showExampleRow ? rowStep : AdminsAnimator.totalAnimatedHeight(animatedEntries, rowStep);
        float targetHeight = (showExampleRow || !animatedEntries.isEmpty()) ? (Admins.CONTENT_START_Y * baseScale + contentHeight) : (Admins.HEADER_HEIGHT * baseScale);
        host.displayWidth = HudRenderUtil.animateDimension(host.displayWidth, maxWidth);
        host.displayHeight = HudRenderUtil.animateDimension(host.displayHeight, targetHeight);
        host.setSize(host.displayWidth, host.displayHeight);
        if (!showWidget && widgetScale <= 0.001f && animatedEntries.isEmpty()) { host.setEmpty(); return; }
        float drawScale = Math.max(0.0f, widgetScale);
        float drawWidth = host.displayWidth * drawScale;
        float drawHeight = host.displayHeight * drawScale;
        if (drawWidth <= 0.0f || drawHeight <= 0.0f) { host.setEmpty(); return; }
        float drawX = host.getX() + ((host.displayWidth - drawWidth) * 0.5f);
        float drawY = host.getY() + ((host.displayHeight - drawHeight) * 0.5f);
        float drawBaseScale = baseScale * drawScale;
        float drawFontScale = fontScale * drawScale;
        float drawHeaderIconH = headerIconH * drawScale;
        float drawHeaderTextH = headerTextH * drawScale;
        float drawRowTextH = rowTextH * drawScale;
        int resolvedHeaderIconColor = host.uiCounter;
        if (host.headerIconPulse.get()) {
            int pulsed = HudTextEffects.animatedColor(host.uiCounter, HudTextEffects.Effect.PULSE, host.headerIconPulseSpeed.get(), (float) (Util.getMillis() / 1000.0), 0.0f);
            resolvedHeaderIconColor = HudRenderUtil.mixColor(host.uiCounter, pulsed, host.headerIconPulseIntensity.get() / 100.0f);
        }
        List<LinkedHashMap<String, Object>> panelRows = ScriptedListHudPanel.rows();
        rowTextRenderer.begin(drawFontScale, false, false);
        if (showExampleRow) {
            panelRows.add(ScriptedListHudPanel.row("preview", "head", ScriptedListHudPanel.idString(AdminsData.resolvePreviewSkin()),
                    List.of(ScriptedListHudPanel.textPart("Example Staff", AdminsPalette.withAlpha(host.uiText & 0x00FFFFFF, 255), 0.0f)),
                    "Vanish", AdminsPalette.withAlpha(AdminsData.resolveStatusColor(host, StaffTracker.Status.VANISH) & 0x00FFFFFF, 255),
                    AdminsPalette.withAlpha(host.uiMuted & 0x00FFFFFF, 130), 1.0f));
        } else {
            for (Admins.AnimatedEntry ae : animatedEntries) {
                float anim = ae.anim(); if (anim <= 0.01f) continue;
                StaffData.StaffEntry entry = ae.entry();
                int rowAlpha = AdminsAnimator.clamp255(Math.round(255.0f * anim));
                int nameBaseColor = AdminsPalette.withAlpha(host.uiText & 0x00FFFFFF, rowAlpha);
                List<LinkedHashMap<String, Object>> parts = AdminsData.legacyParts(rowTextRenderer, entry.displayName(), anim, nameBaseColor);
                panelRows.add(ScriptedListHudPanel.row(entry.name(), "head", ScriptedListHudPanel.idString(AdminsData.resolveSkinByName(entry.name())), parts,
                        AdminsData.formatStatus(entry, now), AdminsPalette.withAlpha(AdminsData.resolveStatusColor(host, entry.status()) & 0x00FFFFFF, rowAlpha),
                        AdminsPalette.withAlpha(host.uiMuted & 0x00FFFFFF, Math.max(26, rowAlpha - 120)), anim));
            }
        }
        rowTextRenderer.end();
        host.scriptedPanel.render(renderer, textRenderer, ctx, tickDelta, new ScriptedListHudPanel.Panel(ScriptedListHudPanel.ADMINS,
                new ScriptedListHudPanel.Palette(host.uiHeaderLeft, host.uiHeaderRight, host.uiBodyLeft, host.uiBodyRight, host.uiOutline, host.uiText, host.uiMuted, host.uiCounter, host.uiTitleText, host.uiDivider, host.uiBlurTint),
                drawX, drawY, drawWidth, drawHeight, drawScale, drawBaseScale, drawFontScale, drawHeaderIconH, drawHeaderTextH, drawRowTextH,
                activeLabelWidth * drawScale, activeCountWidth * drawScale, entries.size(),
                AdminsPalette.hasEffect(host), Math.min(1.0f, (host.blurAlpha.get() / 255.0f) * (AdminsPalette.isThemeMode(host) ? 1.15f : 1.0f)),
                resolvedHeaderIconColor, HudPanelLayoutModes.current(host.layoutMode), panelRows));
    }
}
