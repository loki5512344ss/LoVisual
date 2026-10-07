package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config.ConfigProfilesComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.CosmeticsComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.look.ThemesPageSwitch;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.main.LookSettingsComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.main.MainSettingsComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module.ModuleComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module.SettingsPanelComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.other.BackgroundComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.other.CategoryContainerComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.other.SearchComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations.RelationsComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.theme.ThemeComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.theme.ThemeEditorPreviewComponent;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.features.theme.EditableClickGuiTheme;
import dev.loki.lovisual.render.engine.animation.AnimationUtility;

import java.util.ArrayList;
import java.util.List;

final class MenuScreenState {
    final float inputReadyProgress = 0.72f;
    final float s = 2f;
    final float menuWConst = 400f;
    final float menuHConst = 250f;

    final BackgroundComponent backgroundComponent = new BackgroundComponent();
    final CategoryContainerComponent categoryContainer = new CategoryContainerComponent();
    final SearchComponent searchComponent = new SearchComponent();
    final ModuleComponent moduleComponent = new ModuleComponent();
    final CosmeticsComponent cosmeticsComponent = new CosmeticsComponent();
    final ThemesPageSwitch themesPageSwitch = new ThemesPageSwitch();
    final LookSettingsComponent lookSettings = new LookSettingsComponent();
    final ThemeComponent themeComponent = new ThemeComponent();
    final ThemeEditorPreviewComponent themeEditorPreview = new ThemeEditorPreviewComponent();
    final MainSettingsComponent mainSettingsComponent = new MainSettingsComponent();
    final RelationsComponent relationsComponent = new RelationsComponent();
    final ConfigProfilesComponent configProfilesComponent = new ConfigProfilesComponent();
    final SettingsPanelComponent settingsPanel = new SettingsPanelComponent();

    MenuScreen.Category category = MenuScreen.Category.HUD;
    float menuX;
    float menuY;
    float menuW;
    float menuH;
    float shellX;
    float shellY;
    float shellW;
    float shellH;
    float categoryX;
    float categoryY;
    float searchX;
    float searchY;
    float menuLinePulsePhase;
    float menuLineHSweep;
    float menuLineVSweep;
    float menuLineHDir = 1f;
    float menuLineVDir = 1f;
    float menuMaskAnim;
    float screenAnim;
    boolean openTarget;
    EditableClickGuiTheme activeThemeEditor;

    boolean isInteractive() {
        return openTarget && screenAnim >= inputReadyProgress;
    }

    boolean isVisible() {
        return openTarget || screenAnim > 0.001f;
    }

    float easeOutCubic(float t) {
        t = AnimationUtility.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv;
    }

    float easeOutQuint(float t) {
        t = AnimationUtility.clamp(t, 0.0f, 1.0f);
        float inv = 1.0f - t;
        return 1.0f - inv * inv * inv * inv * inv;
    }

    float smootherStep(float t) {
        t = AnimationUtility.clamp(t, 0.0f, 1.0f);
        return t * t * t * (t * (t * 6.0f - 15.0f) + 10.0f);
    }

    List<ModuleComponent.CardEntry> visibleEntries(List<ModuleComponent.CardEntry> src) {
        if (!ClickGuiSearch.isActive() || ClickGuiSearch.getText().isEmpty()) {
            return src;
        }
        List<ModuleComponent.CardEntry> out = new ArrayList<>();
        for (ModuleComponent.CardEntry e : src) {
            if (ClickGuiSearch.matches(e.title(), e.searchAliases())) {
                out.add(e);
            }
        }
        return out;
    }
}
