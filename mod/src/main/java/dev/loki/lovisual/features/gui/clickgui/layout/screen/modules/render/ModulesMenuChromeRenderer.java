/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.layout.screen.modules;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.modules.model.ModulesMenuStyle;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module.ModuleComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderContext;
import dev.loki.lovisual.features.gui.clickgui.settings.render.SettingRenderSurface;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.tooltip.ClickGuiHintOverlay;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import dev.loki.lovisual.render.helpers.clip.ScissorFunction;

final class ModulesMenuChromeRenderer {
    private ModulesMenuChromeRenderer() {}

    static TextRenderer regular;
    static TextRenderer medium;
    static TextRenderer semibold;

    static void ensureFonts() {
        if (regular == null) regular = Fonts.renderer("Onest", FontInfo.Type.Regular, ClickGuiRenderer.getInterRegular());
        if (medium == null) medium = Fonts.renderer("OnestMedium", FontInfo.Type.Regular, regular);
        if (semibold == null) semibold = Fonts.renderer("OnestBold", FontInfo.Type.Regular, medium);
    }

    static TextRenderer regularForRow() { ensureFonts(); return regular; }
    static TextRenderer semiboldForRow() { ensureFonts(); return semibold; }

    static String bindingLabel(ModuleComponent.CardEntry entry) {
        String pending = ClickGuiRenderer.getPendingBindDisplay();
        if (pending != null) return pending;
        String bind = entry.bindLabel();
        return bind == null || bind.isBlank() ? "Binding..." : "Key: " + bind;
    }

    static void renderPanel(ModulesMenuPanel panel, float mouseX, float mouseY, float alpha, float scale) {
        panel.update();
        int text = ModulesMenuLayout.withAlpha(ModulesMenuStyle.text(), alpha);
        int muted = ModulesMenuLayout.withAlpha(ModulesMenuStyle.textMuted(), alpha);
        int split = ModulesMenuLayout.withAlpha(ModulesMenuStyle.split(), alpha);
        int veil = ModulesMenuLayout.withAlpha(ModulesMenuStyle.panelBgGlassDark(), alpha * 0.30f);
        LayoutRender2D.roundedQuad(panel.x, panel.y, panel.w, panel.h, ModulesMenuScreen.PANEL_RADIUS * scale, veil, veil, veil, veil);
        panel.hits.clear();
        panel.settingHits.clear();
        float titleY = panel.y + ModulesMenuLayout.middle(ModulesMenuLayout.textHeight(semibold, 9.0f * scale), ModulesMenuScreen.HEADER_H * scale) + 0.5f * scale;
        ClickGuiRenderer.drawText(semibold, panel.category.title(), panel.x + ModulesMenuScreen.TEXT_LEFT_PADDING * scale, titleY, 9.0f * scale, text, false);
        ModulesMenuGlassRenderer.drawCategoryIcon(panel, muted, scale);
        if (panel.swap < 0.999f) {
            ModulesMenuModuleRenderer.renderModulePage(panel, mouseX, mouseY, alpha * (1.0f - panel.swap), scale);
        }
        if (panel.swap > 0.001f) {
            renderSettingsPage(panel, mouseX, mouseY, alpha * panel.swap, scale);
        }
        LayoutRender2D.rect(panel.x + 8.0f * scale, panel.y + ModulesMenuScreen.HEADER_H * scale, panel.w - 16.0f * scale, 0.5f * scale, split);
    }

    static void renderSettingsPage(ModulesMenuPanel panel, float mouseX, float mouseY, float alpha, float scale) {
        if (alpha <= 0.001f) return;
        float pageX = panel.x + panel.w * (1.0f - panel.swap);
        float settingsPadLeft = 6.0f * scale;
        float settingsPadRight = 13.0f * scale;
        float settingsX = pageX + settingsPadLeft;
        float settingsW = Math.max(1.0f, panel.w - settingsPadLeft - settingsPadRight);
        float settingsTop = panel.y + (ModulesMenuScreen.HEADER_H + ModulesMenuScreen.SEPARATOR_H) * scale;
        float backRowY = panel.y + 28.0f * scale;
        float backRowH = 20.0f * scale;
        boolean pageClip = ScissorFunction.pushRaw(panel.x, panel.y, panel.w, panel.h);
        boolean headerClip = ScissorFunction.pushRaw(panel.x, panel.y, panel.w, panel.h);
        if (ModulesMenuLayout.inside(mouseX, mouseY, pageX, backRowY, panel.w, backRowH)) {
            int hoverBg = ModulesMenuLayout.withAlpha(ModulesMenuStyle.rowHover(), alpha);
            LayoutRender2D.roundedQuad(pageX + 3.0f * scale, backRowY + scale, panel.w - 6.0f * scale, backRowH - 2.0f * scale, 5.0f * scale, hoverBg, hoverBg, hoverBg, hoverBg);
        }
        String title = panel.selectedTitle == null ? "Settings" : panel.selectedTitle;
        title = ClickGuiRenderer.fitText(regular, title, 8.0f * scale, panel.w - 18.0f * scale);
        ClickGuiRenderer.drawText(regular, title, pageX + ModulesMenuScreen.TEXT_LEFT_PADDING * scale,
                settingsTop + ModulesMenuLayout.middle(ModulesMenuLayout.textHeight(regular, 8.0f * scale), ModulesMenuScreen.HEADER_H * scale) - scale,
                8.0f * scale, ModulesMenuLayout.withAlpha(ModulesMenuStyle.text(), alpha), false);
        if (headerClip) ScissorFunction.pop();
        float clipY = settingsTop + ModulesMenuScreen.HEADER_H * scale;
        float clipH = panel.h - ModulesMenuScreen.HEADER_H * 2.0f * scale - ModulesMenuScreen.SEPARATOR_H * scale - 0.5f * scale - 5.0f * scale;
        boolean clipped = ScissorFunction.pushRaw(settingsX, clipY, settingsW, clipH);
        float y = clipY - panel.settingsSmoothScroll;
        float total = 0.0f;
        try (SettingRenderContext.Scope ignored = SettingRenderContext.push(SettingRenderSurface.MODULES, scale)) {
            for (Setting setting : panel.selectedSettings) {
                float vis = setting.updateVisibilityAnim();
                if (!setting.isVisibilityTargetVisible() && vis <= 0.01f) continue;
                float baseH = setting.getHeight();
                float h = baseH * vis;
                if (h > 0.5f) {
                    panel.settingHits.add(new ModulesMenuPanel.SettingHit(setting, settingsX, y, settingsW, h));
                    if (y + h >= clipY && y <= clipY + clipH) {
                        float slide = (-baseH + baseH * vis) * 0.5f;
                        boolean settingClipped = ScissorFunction.pushRaw(settingsX, y, settingsW, h);
                        setting.render(settingsX, y + slide, settingsW, mouseX, mouseY);
                        if (settingClipped) ScissorFunction.pop();
                    }
                }
                y += h;
                total += h;
            }
        }
        panel.maxSettingsScroll = Math.max(0.0f, total - clipH);
        panel.settingsScroll = AnimationUtility.clamp(panel.settingsScroll, 0.0f, panel.maxSettingsScroll);
        panel.settingsSmoothScroll = AnimationUtility.approach(panel.settingsSmoothScroll, panel.settingsScroll, 0.20f);
        panel.settingsSmoothScroll = AnimationUtility.snap(panel.settingsSmoothScroll, panel.settingsScroll, 0.05f);
        if (clipped) ScissorFunction.pop();
        ModulesMenuGlassRenderer.renderScrollbar(panel, panel.settingsSmoothScroll, panel.maxSettingsScroll, clipY, clipH, alpha, mouseX, mouseY, scale);
        if (pageClip) ScissorFunction.pop();
    }

    static void renderSearchButton(ModulesMenuScreen screen, float mouseX, float mouseY, float screenAnim, float scale) {
        float alpha = ModulesMenuLayout.easeOutCubic(screenAnim);
        if (alpha <= 0.001f) return;
        boolean hovered = ModulesMenuLayout.inside(mouseX, mouseY, screen.searchBtnX, screen.searchBtnY, screen.searchBtnW, screen.searchBtnH);
        float dt = AnimationUtility.deltaTime();
        screen.searchBtnHoverAnim = AnimationUtility.approach(screen.searchBtnHoverAnim, hovered ? 1.0f : 0.0f, dt, 12.0f);
        screen.searchBtnHoverAnim = AnimationUtility.snap(screen.searchBtnHoverAnim, hovered ? 1.0f : 0.0f, 0.01f);
        int bg = ModulesMenuLayout.withAlpha(ModulesMenuStyle.panelBgGlassDark(), alpha * (0.45f + 0.35f * screen.searchBtnHoverAnim));
        LayoutRender2D.roundedQuad(screen.searchBtnX, screen.searchBtnY, screen.searchBtnW, screen.searchBtnH, 6.0f * scale, bg, bg, bg, bg);
        LayoutRender2D.roundedStrokeQuad(screen.searchBtnX, screen.searchBtnY, screen.searchBtnW, screen.searchBtnH, 6.0f * scale, 0.5f * scale,
                ModulesMenuLayout.withAlpha(ModulesMenuStyle.textMuted(), alpha * 0.35f),
                ModulesMenuLayout.withAlpha(ModulesMenuStyle.textMuted(), alpha * 0.35f),
                ModulesMenuLayout.withAlpha(ModulesMenuStyle.textMuted(), alpha * 0.35f),
                ModulesMenuLayout.withAlpha(ModulesMenuStyle.textMuted(), alpha * 0.35f));
        TextRenderer icons = Fonts.renderer("Icons", FontInfo.Type.Regular, ClickGuiRenderer.getInterRegular());
        float iconSize = 9.0f * scale;
        float iconX = screen.searchBtnX + (screen.searchBtnW - ClickGuiRenderer.textWidth(icons, "s", iconSize)) * 0.5f;
        float iconY = screen.searchBtnY + ModulesMenuLayout.middle(ModulesMenuLayout.textHeight(icons, iconSize), screen.searchBtnH);
        int iconColor = ModulesMenuLayout.withAlpha(ClickGuiSearch.isActive() ? 0xFFF5F5FF : hovered ? 0xFFD0D0DC : 0xFF878894, alpha);
        ClickGuiRenderer.drawText(icons, "s", iconX, iconY, iconSize, iconColor, false);
    }

    static void renderSearch(ModulesMenuScreen screen, float screenAnim, float scale) {
        if (screen.searchAnim <= 0.001f && !ClickGuiSearch.isActive()) return;
        float alpha = ModulesMenuLayout.easeOutCubic(screenAnim) * screen.searchAnim;
        if (alpha <= 0.001f) return;
        float materialAlpha = alpha >= 0.999f ? 1.0f : alpha;
        float radius = 6.0f * scale;
        ModulesMenuGlassRenderer.drawLiquidGlass(screen.searchX, screen.searchY, screen.searchW, screen.searchH, radius, false, materialAlpha, scale);
        int veil = ModulesMenuLayout.withAlpha(ModulesMenuStyle.panelBgGlassDark(), alpha * 0.25f);
        LayoutRender2D.roundedQuad(screen.searchX, screen.searchY, screen.searchW, screen.searchH, radius, veil, veil, veil, veil);
        String text = ClickGuiSearch.getText();
        String draw = text == null || text.isBlank() ? "Search..." : text;
        int color = text == null || text.isBlank() ? ModulesMenuLayout.withAlpha(ModulesMenuStyle.textFaint(), alpha) : ModulesMenuLayout.withAlpha(ModulesMenuStyle.text(), alpha);
        ensureFonts();
        ClickGuiRenderer.drawText(regular, draw, screen.searchX + 8.0f * scale,
                screen.searchY + ModulesMenuLayout.middle(ModulesMenuLayout.textHeight(regular, 8.0f * scale), screen.searchH),
                8.0f * scale, color, false);
    }

    static void renderHints(ModulesMenuScreen screen, float screenAnim, float scale) {
        MainConfig config = MainConfig.get();
        if (!config.isClickGuiHintsEnabled()) return;
        float alpha = ModulesMenuLayout.easeOutCubic(screenAnim);
        ClickGuiHintOverlay.renderBottomLeft(screen.areaX, screen.areaY, screen.areaW, screen.areaH, scale, alpha,
                ClickGuiI18n.tr("clickgui.hints.modules.toggle", "LMB - toggle module"),
                ClickGuiI18n.tr("clickgui.hints.modules.settings", "RMB - settings"),
                ClickGuiI18n.tr("clickgui.hints.modules.module_list", "E - edit ModuleList"),
                ClickGuiI18n.tr("clickgui.hints.modules.search", "Ctrl+F - search"),
                ClickGuiI18n.tr("clickgui.hints.modules.close", "Esc - close ClickGui"),
                ClickGuiI18n.tr("clickgui.hints.modules.hide", "Alt+H - hide hints"));
    }
}
