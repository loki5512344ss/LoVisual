package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings;

import dev.loki.lovisual.config.i18n.MainConfig;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.CosmeticsComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.look.ThemesPage;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module.ModuleComponent;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.theme.ThemeComponent;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.tabs.ClickGuiSearch;
import dev.loki.lovisual.features.theme.EditableClickGuiTheme;
import dev.loki.lovisual.features.theme.Theme;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Path;
import java.util.List;

final class MenuScreenInput {
    boolean isHintToggle(int keyCode, int modifiers) {
        return keyCode == GLFW.GLFW_KEY_H && (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }
    boolean mouseClicked(MenuScreenState state, float mx, float my, int button) {
        if (!state.isInteractive() || (button != 0 && button != 1)) return false;
        if (state.activeThemeEditor != null && state.themeEditorPreview.mouseClicked(state.activeThemeEditor, mx, my, button)) {
            state.settingsPanel.close();
            state.activeThemeEditor = null;
            state.themeEditorPreview.reset();
            return true;
        }
        if (state.settingsPanel.mouseClicked(mx, my, button, state.s)) return true;
        if (state.settingsPanel.blocksMenuInput()) return true;
        if (state.searchComponent.click(state.searchX, state.searchY, 80f * state.s, 15f * state.s, mx, my, button)) return true;
        MenuScreen.Category hit = state.categoryContainer.click(state.categoryX, state.categoryY, mx, my, state.s);
        if (hit != null) {
            if (hit != state.category) {
                state.category = hit;
                state.moduleComponent.resetScroll();
                state.themeComponent.resetScroll();
                state.lookSettings.resetScroll();
                state.cosmeticsComponent.resetScroll();
                state.mainSettingsComponent.resetScroll();
                state.relationsComponent.resetScroll();
                state.configProfilesComponent.resetScroll();
            }
            return true;
        }
        if (state.category == MenuScreen.Category.COSMETICS) {
            return state.cosmeticsComponent.click(mx, my, button, module -> openModuleSettings(state, module));
        }
        if (state.category == MenuScreen.Category.THEMES && state.themesPageSwitch.click(mx, my, button)) return true;
        if (state.category == MenuScreen.Category.THEMES && state.themesPageSwitch.page() != ThemesPage.THEMES) {
            if (state.lookSettings.mousePressedScrollbar(mx, my, button)) return true;
            return state.lookSettings.mouseClicked(mx, my, button);
        }
        if (state.category == MenuScreen.Category.THEMES) {
            if (state.themeComponent.mousePressedScrollbar(mx, my, button)) return true;
            ThemeComponent.ThemeAction action = state.themeComponent.click(mx, my, button);
            if (action == null) return false;
            handleThemeAction(state, action);
            return true;
        }
        if (state.category == MenuScreen.Category.CONFIGS) {
            if (state.configProfilesComponent.mousePressedScrollbar(mx, my, button)) return true;
            return state.configProfilesComponent.click(mx, my, button);
        }
        if (state.category == MenuScreen.Category.MAIN_SETTINGS) {
            if (state.mainSettingsComponent.mousePressedScrollbar(mx, my, button)) return true;
            return state.mainSettingsComponent.mouseClicked(mx, my, button);
        }
        if (state.category == MenuScreen.Category.RELATIONS) {
            if (state.relationsComponent.mousePressedScrollbar(mx, my, button)) return true;
            return state.relationsComponent.click(mx, my, button);
        }
        if (state.moduleComponent.mousePressedScrollbar(mx, my, button)) return true;
        ModuleComponent.CardAction action = state.moduleComponent.click(mx, my, button);
        if (action == null) return false;
        if (action.toggleable() && (action.statusClick() || action.leftClick())) toggleEntry(action.getId());
        if (action.rightClick()) openSettings(state, action.getId());
        return true;
    }
    void mouseReleased(MenuScreenState state, float mx, float my, int button) {
        if (!state.isInteractive()) return;
        state.settingsPanel.mouseReleased(mx, my, button);
        state.cosmeticsComponent.mouseReleased();
        state.lookSettings.mouseReleased(mx, my, button);
        state.themeComponent.mouseReleased(button);
        state.mainSettingsComponent.mouseReleased(mx, my, button);
        state.relationsComponent.mouseReleased(button);
        state.configProfilesComponent.mouseReleased(button);
        state.moduleComponent.mouseReleased(button);
    }
    void mouseScrolled(MenuScreenState state, float mx, float my, double delta) {
        if (!state.isInteractive()) return;
        if (state.settingsPanel.mouseScrolled(mx, my, delta)) return;
        if (state.settingsPanel.blocksMenuInput()) return;
        if (state.category == MenuScreen.Category.THEMES && state.themesPageSwitch.page() != ThemesPage.THEMES) {
            state.lookSettings.scroll(mx, my, delta);
            return;
        }
        if (state.category == MenuScreen.Category.THEMES) {
            state.themeComponent.scroll(mx, my, state.menuX, state.menuY, state.menuW, state.menuH, delta, state.s);
            return;
        }
        if (state.category == MenuScreen.Category.COSMETICS) { state.cosmeticsComponent.scroll(delta, state.s); return; }
        if (state.category == MenuScreen.Category.MAIN_SETTINGS) { state.mainSettingsComponent.scroll(mx, my, delta); return; }
        if (state.category == MenuScreen.Category.RELATIONS) { state.relationsComponent.scroll(mx, my, delta); return; }
        if (state.category == MenuScreen.Category.CONFIGS) {
            state.configProfilesComponent.scroll(mx, my, state.menuX, state.menuY, state.menuW, state.menuH, delta, state.s);
            return;
        }
        state.moduleComponent.scroll(mx, my, state.menuX, state.menuY, state.menuW, state.menuH, delta, state.s);
    }
    boolean keyPressed(MenuScreenState state, int keyCode, int scanCode, int modifiers) {
        if (!state.isInteractive()) return false;
        if (state.category == MenuScreen.Category.COSMETICS && state.cosmeticsComponent.keyPressed(keyCode)) return true;
        if (state.settingsPanel.isActive() && state.settingsPanel.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (isHintToggle(keyCode, modifiers)) {
            MainConfig config = MainConfig.get();
            config.setClickGuiHintsEnabled(!config.isClickGuiHintsEnabled());
            return true;
        }
        if (state.category == MenuScreen.Category.MAIN_SETTINGS && state.mainSettingsComponent.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (state.category == MenuScreen.Category.CONFIGS && state.configProfilesComponent.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (state.category == MenuScreen.Category.RELATIONS && state.relationsComponent.keyPressed(keyCode, scanCode, modifiers)) return true;
        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (ctrl && keyCode == GLFW.GLFW_KEY_F) { ClickGuiSearch.setActive(true); return true; }
        if (ClickGuiSearch.isActive()) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { ClickGuiSearch.deactivate(); return true; }
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) { ClickGuiSearch.backspace(); return true; }
            return true;
        }
        return false;
    }
    boolean charTyped(MenuScreenState state, char chr, int modifiers) {
        if (!state.isInteractive()) return false;
        if (state.category == MenuScreen.Category.MAIN_SETTINGS && state.mainSettingsComponent.charTyped(chr, modifiers)) return true;
        if (state.category == MenuScreen.Category.CONFIGS && state.configProfilesComponent.charTyped(chr, modifiers)) return true;
        if (state.category == MenuScreen.Category.RELATIONS && state.relationsComponent.charTyped(chr, modifiers)) return true;
        if (ClickGuiSearch.isActive()) { ClickGuiSearch.append(chr); return true; }
        return state.settingsPanel.charTyped(chr, modifiers);
    }
    boolean onFilesDrop(MenuScreenState state, List<Path> paths) {
        if (!state.isInteractive()) return false;
        if (state.category == MenuScreen.Category.COSMETICS) {
            return state.cosmeticsComponent.onFilesDrop(paths, ClickGuiRenderer.getMouseX(), ClickGuiRenderer.getMouseY());
        }
        if (state.category != MenuScreen.Category.CONFIGS) return false;
        return state.configProfilesComponent.importDroppedFiles(paths);
    }
    private void handleThemeAction(MenuScreenState state, ThemeComponent.ThemeAction action) {
        if (action.type() == ThemeComponent.ThemeActionType.SELECT && action.themeId() != null) { Theme.setTheme(action.themeId()); return; }
        if (action.type() == ThemeComponent.ThemeActionType.ADD) { openThemeEditor(state, EditableClickGuiTheme.createFromCurrent()); return; }
        if (action.type() == ThemeComponent.ThemeActionType.EDIT && action.themeId() != null) { openThemeEditor(state, EditableClickGuiTheme.existing(action.themeId())); return; }
        if (action.type() == ThemeComponent.ThemeActionType.DELETE && action.themeId() != null) Theme.deleteCustomTheme(action.themeId());
    }
    private void toggleEntry(String id) { MenuSettingsResolver.toggleEntry(id); }
    private void openSettings(MenuScreenState state, String id) {
        MenuSettingsResolver.ResolvedSettings resolved = MenuSettingsResolver.resolveSettings(id);
        if (resolved == null || resolved.settings().isEmpty()) return;
        state.settingsPanel.open(resolved.getId(), resolved.title(), resolved.settings(), resolved.hudContext());
    }
    private void openModuleSettings(MenuScreenState state, Module module) {
        List<Setting> settings = CosmeticsComponent.settingsOf(module);
        if (settings.isEmpty()) return;
        state.settingsPanel.open(module.name(), module.getDisplayName(), settings, false);
    }
    void openThemeEditor(MenuScreenState state, EditableClickGuiTheme editor) {
        if (editor == null) return;
        state.activeThemeEditor = editor;
        state.themeEditorPreview.reset();
        state.settingsPanel.openEditor("theme:" + editor.id(), editor.title(), editor.buildSettings());
    }
}
