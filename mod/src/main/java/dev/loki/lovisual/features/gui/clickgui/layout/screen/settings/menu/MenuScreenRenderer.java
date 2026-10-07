package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module.ModuleComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.look.ThemesPage;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.tooltip.ClickGuiHintOverlay;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.warp.RenderWarpStack;
import dev.loki.lovisual.render.engine.svg.model.SvgRenderOptions;
import dev.loki.lovisual.render.engine.text.font.Fonts;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import java.util.List;

final class MenuScreenRenderer {
    private final MenuScreenLayout layout;
    MenuScreenRenderer(MenuScreenLayout layout) { this.layout = layout; }
    void render(MenuScreenState state, float areaX, float areaY, float areaW, float areaH, float mx, float my) {
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        float dt = AnimationUtility.deltaTime();
        state.screenAnim = AnimationUtility.approach(state.screenAnim, state.openTarget ? 1.0f : 0.0f, dt, state.openTarget ? 8.5f : 7.2f);
        state.screenAnim = AnimationUtility.snap(state.screenAnim, state.openTarget ? 1.0f : 0.0f, 0.002f);
        if (state.screenAnim <= 0.001f && !state.openTarget) return;
        layout.layout(state, areaX, areaY, areaW, areaH);
        boolean hudPanelActive = state.settingsPanel.shouldMaskMenu();
        float maskTarget = hudPanelActive ? 1f : 0f;
        state.menuMaskAnim = AnimationUtility.approach(state.menuMaskAnim, maskTarget, dt, 10f);
        state.menuMaskAnim = AnimationUtility.snap(state.menuMaskAnim, maskTarget, 0.01f);
        boolean renderMain = !hudPanelActive;
        if (state.activeThemeEditor != null && !state.settingsPanel.isActive()) {
            state.activeThemeEditor = null;
            state.themeEditorPreview.reset();
        }
        float distanceProgress = state.easeOutQuint(state.screenAnim);
        float fadeProgress = state.smootherStep(state.screenAnim);
        float previousAlpha = ClickGuiRenderer.setRenderAlphaMultiplier(fadeProgress);
        Renderer2D.COLOR.setAlpha(fadeProgress);
        try (RenderWarpStack.Scope ignored = layout.pushLifecycleWarp(state, distanceProgress)) {
            if (renderMain) {
                renderPanelShadow(state, palette);
                state.backgroundComponent.render(state.shellX, state.shellY, state.shellW, state.shellH, state.s);
                renderShellChrome(state, palette);
                state.categoryContainer.render(state.categoryX, state.categoryY, mx, my, state.category, state.s);
                state.searchComponent.render(state.searchX, state.searchY, 80f * state.s, 15f * state.s);
                renderCategoryContent(state, mx, my);
            }
            if (renderMain && state.menuMaskAnim > 0.01f) {
                int c0 = LayoutRender2D.alpha(palette.menuMaskLeft(), state.menuMaskAnim * fadeProgress);
                int c1 = LayoutRender2D.alpha(palette.menuMaskRight(), state.menuMaskAnim * fadeProgress);
                LayoutRender2D.roundedQuad(state.shellX, state.shellY, state.shellW, state.shellH, 8f * state.s, c0, c1, c1, c0);
            }
            if (state.activeThemeEditor != null && state.settingsPanel.isActive()) {
                state.themeEditorPreview.render(state.activeThemeEditor, state.menuX, state.menuY, state.menuW, state.menuH, mx, my, state.s);
            }
            state.settingsPanel.render(state.menuX, state.menuY, state.menuW, state.menuH, mx, my, state.s);
        } finally {
            Renderer2D.COLOR.setAlpha(1.0f);
            ClickGuiRenderer.restoreRenderAlphaMultiplier(previousAlpha);
        }
        if (renderMain && !state.settingsPanel.isActive()) renderContextHints(state, areaX, areaY, areaW, areaH, fadeProgress);
    }
    private void renderCategoryContent(MenuScreenState state, float mx, float my) {
        if (state.category == MenuScreen.Category.COSMETICS) { state.cosmeticsComponent.render(state.menuX, state.menuY, state.menuW, state.menuH, mx, my, state.s); return; }
        if (state.category == MenuScreen.Category.THEMES) {
            state.themesPageSwitch.render(state.menuX, state.menuY, state.menuW, state.s);
            if (state.themesPageSwitch.page() == ThemesPage.THEMES) state.themeComponent.render(state.menuX, state.menuY, state.menuW, state.menuH, mx, my, state.s);
            else state.lookSettings.render(state.themesPageSwitch.page(), state.menuX, state.menuY, state.menuW, state.menuH, mx, my, state.s);
            return;
        }
        if (state.category == MenuScreen.Category.MAIN_SETTINGS) { state.mainSettingsComponent.render(state.menuX, state.menuY, state.menuW, state.menuH, mx, my, state.s); return; }
        if (state.category == MenuScreen.Category.RELATIONS) { state.relationsComponent.render(state.menuX, state.menuY, state.menuW, state.menuH, mx, my, state.s); return; }
        if (state.category == MenuScreen.Category.CONFIGS) { state.configProfilesComponent.render(state.menuX, state.menuY, state.menuW, state.menuH, mx, my, state.s); return; }
        List<ModuleComponent.CardEntry> entries = state.visibleEntries(MenuSettingsResolver.buildCards(state.category));
        state.moduleComponent.render(state.menuX, state.menuY, state.menuW, state.menuH, entries, state.s);
    }
    private void renderPanelShadow(MenuScreenState state, SettingsGuiPalette palette) {
        LayoutRender2D.roundedSoftShadow(state.shellX, state.shellY, state.shellW, state.shellH, 8f * state.s, 8f * state.s, 0.018f, palette.menuShadow());
    }
    void renderShellChrome(MenuScreenState state, SettingsGuiPalette palette) {
        float vLineX = state.shellX + 42.5f * state.s;
        float hLineY = state.shellY + 28f * state.s;
        float lineThickness = 0.5f * state.s;
        float vLineH = state.shellH;
        float hLineW = state.shellW - 43f * state.s;
        float hSegW = 56f * state.s;
        float vSegH = 52f * state.s;
        float hTravel = Math.max(1f, hLineW - hSegW);
        float vTravel = Math.max(1f, vLineH - vSegH);
        float dt = AnimationUtility.deltaTime();
        LayoutRender2D.rectQuad(vLineX, state.shellY, lineThickness, vLineH, palette.menuLineLow(), palette.menuLineMid(), palette.menuLineLow(), palette.menuLineStrong());
        LayoutRender2D.rectQuad(state.shellX + 43f * state.s, hLineY, hLineW, lineThickness, palette.menuLineStrong(), palette.menuLineLow(), palette.menuLineStrong(), palette.menuLineLow());
        state.menuLineHSweep = AnimationUtility.clamp(state.menuLineHSweep + state.menuLineHDir * 120f * dt, 0f, hTravel);
        if (state.menuLineHSweep >= hTravel) state.menuLineHDir = -1f; else if (state.menuLineHSweep <= 0f) state.menuLineHDir = 1f;
        state.menuLineVSweep = AnimationUtility.clamp(state.menuLineVSweep + state.menuLineVDir * 95f * dt, 0f, vTravel);
        if (state.menuLineVSweep >= vTravel) state.menuLineVDir = -1f; else if (state.menuLineVSweep <= 0f) state.menuLineVDir = 1f;
        state.menuLinePulsePhase += 4.1f * dt;
        float pulse = 0.65f + 0.35f * (float) Math.sin(state.menuLinePulsePhase);
        float hSegX = state.shellX + 43f * state.s + state.menuLineHSweep;
        int hA0 = LayoutRender2D.alpha(palette.menuSweepStart(), pulse);
        int hA1 = LayoutRender2D.alpha(palette.menuSweepEnd(), pulse);
        LayoutRender2D.rectQuad(hSegX, hLineY, hSegW, lineThickness, hA0, hA1, hA1, hA0);
        float vSegY = state.shellY + state.menuLineVSweep;
        int vA0 = LayoutRender2D.alpha(palette.menuSweepStart(), pulse * 0.86f);
        int vA1 = LayoutRender2D.alpha(palette.menuSweepEnd(), pulse * 0.93f);
        LayoutRender2D.rectQuad(vLineX, vSegY, lineThickness, vSegH, vA0, vA1, vA0, vA1);
        renderHeaderTitle(state, palette);
    }
    private void renderHeaderTitle(MenuScreenState state, SettingsGuiPalette palette) {
        float baseX = state.shellX + 50f * state.s;
        float baseY = state.shellY + 13.5f * state.s;
        float iconSize = 11.5f * state.s;
        float textSize = 11.5f * state.s;
        if (state.category.iconToken()) {
            TextRenderer icons = Fonts.renderer("Icons", FontInfo.Type.Regular, ClickGuiRenderer.getInterRegular());
            ClickGuiRenderer.drawText(icons, state.category.token(), baseX, baseY, iconSize, palette.menuHeaderText(), false);
            float iconW = ClickGuiRenderer.textWidth(icons, state.category.token(), iconSize);
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), " " + state.category.title(), baseX + iconW + 1.5f * state.s, baseY, textSize, palette.menuHeaderText(), false);
            return;
        }
        if (state.category.svgIcon()) {
            Renderer2D.COLOR.svg(state.category.token(), baseX, baseY + 0.5f * state.s, iconSize, iconSize, SvgRenderOptions.overrideColor(palette.menuHeaderText()));
            ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), " " + state.category.title(), baseX + iconSize + 1.5f * state.s, baseY, textSize, palette.menuHeaderText(), false);
            return;
        }
        ClickGuiRenderer.drawText(ClickGuiRenderer.getInterRegular(), "| " + state.category.title(), baseX, baseY, textSize, palette.menuHeaderText(), false);
    }
    private void renderContextHints(MenuScreenState state, float areaX, float areaY, float areaW, float areaH, float alpha) {
        MainConfig config = MainConfig.get();
        if (!config.isClickGuiHintsEnabled()) return;
        String primary; String secondary; String tertiary;
        switch (state.category) {
            case HUD -> { primary = ClickGuiI18n.tr("clickgui.hints.settings.hud.toggle", "LMB - toggle HUD element"); secondary = ClickGuiI18n.tr("clickgui.hints.settings.hud.settings", "RMB - open element settings"); tertiary = ClickGuiI18n.tr("clickgui.hints.settings.hud.drag", "Open settings - edit widget position"); }
            case UI -> { primary = ClickGuiI18n.tr("clickgui.hints.settings.ui.toggle", "LMB - toggle UI element"); secondary = ClickGuiI18n.tr("clickgui.hints.settings.ui.settings", "RMB - open element settings"); tertiary = ClickGuiI18n.tr("clickgui.hints.settings.ui.search", "Ctrl+F - search cards"); }
            case COSMETICS -> { primary = ClickGuiI18n.tr("clickgui.hints.settings.cosmetics.toggle", "LMB - wear / take off"); secondary = ClickGuiI18n.tr("clickgui.hints.settings.cosmetics.settings", "RMB - open settings"); tertiary = ClickGuiI18n.tr("clickgui.hints.settings.cosmetics.kind", "Top pills - choose kind"); }
            case THEMES -> { primary = ClickGuiI18n.tr("clickgui.hints.settings.themes.select", "LMB - select theme"); secondary = ClickGuiI18n.tr("clickgui.hints.settings.themes.edit", "Edit - open theme editor"); tertiary = ClickGuiI18n.tr("clickgui.hints.settings.themes.scroll", "Wheel - scroll themes"); }
            case MAIN_SETTINGS -> { primary = ClickGuiI18n.tr("clickgui.hints.settings.main.select", "LMB - change setting"); secondary = ClickGuiI18n.tr("clickgui.hints.settings.main.category", "Left list - switch setting group"); tertiary = ClickGuiI18n.tr("clickgui.hints.settings.main.scroll", "Wheel - scroll settings"); }
            case RELATIONS -> { primary = ClickGuiI18n.tr("clickgui.hints.settings.relations.mode", "Top pills - choose relation list"); secondary = ClickGuiI18n.tr("clickgui.hints.settings.relations.add", "User-plus icon - pick online player"); tertiary = ClickGuiI18n.tr("clickgui.hints.settings.relations.search", "Search - filter names"); }
            case CONFIGS -> { primary = ClickGuiI18n.tr("clickgui.hints.settings.configs.select", "LMB - select config"); secondary = ClickGuiI18n.tr("clickgui.hints.settings.configs.rename", "Click name - rename config"); tertiary = ClickGuiI18n.tr("clickgui.hints.settings.configs.enter", "Enter/Esc - confirm or cancel text edit"); }
            default -> { primary = ClickGuiI18n.tr("clickgui.hints.settings.generic.select", "LMB - select"); secondary = ClickGuiI18n.tr("clickgui.hints.settings.generic.scroll", "Wheel - scroll"); tertiary = ClickGuiI18n.tr("clickgui.hints.settings.generic.search", "Ctrl+F - search"); }
        }
        ClickGuiHintOverlay.renderBottomLeft(areaX, areaY, areaW, areaH, state.s, alpha, primary, secondary, tertiary,
                ClickGuiI18n.tr("clickgui.hints.settings.close", "Esc - close ClickGUI"),
                ClickGuiI18n.tr("clickgui.hints.settings.hide", "Alt+H - hide hints"));
    }
}
