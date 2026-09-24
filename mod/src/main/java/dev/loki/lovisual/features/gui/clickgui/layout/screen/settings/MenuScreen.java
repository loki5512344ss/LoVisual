package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;

import java.nio.file.Path;
import java.util.List;

public final class MenuScreen {
    private final MenuScreenState state;
    private final MenuScreenLayout layout;
    private final MenuScreenRenderer renderer;
    private final MenuScreenInput input;

    public MenuScreen() {
        MenuScreenState createdState = new MenuScreenState();
        MenuScreenLayout createdLayout = new MenuScreenLayout();
        MenuScreenRenderer createdRenderer = new MenuScreenRenderer(createdLayout);
        MenuScreenInput createdInput = new MenuScreenInput();
        this.state = createdState;
        this.layout = createdLayout;
        this.renderer = createdRenderer;
        this.input = createdInput;
    }

    public void clearScreens() {
    }

    public void markDirty() {
    }

    public void open() {
        state.openTarget = true;
    }

    public void close() {
        state.openTarget = false;
        state.settingsPanel.mouseReleased(0f, 0f, 0);
        state.relationsComponent.resetScroll();
        state.activeThemeEditor = null;
        state.themeEditorPreview.reset();
        if (!ClickGuiRenderer.isClosingForExit()) {
            state.screenAnim = 0.0f;
        }
    }

    public boolean isVisible() {
        return state.isVisible();
    }

    public void render(float areaX, float areaY, float areaW, float areaH, float mx, float my) {
        renderer.render(state, areaX, areaY, areaW, areaH, mx, my);
    }

    public boolean mouseClicked(float mx, float my, int button) {
        return input.mouseClicked(state, mx, my, button);
    }

    public void mouseReleased(float mx, float my, int button) {
        input.mouseReleased(state, mx, my, button);
    }

    public void mouseScrolled(float mx, float my, double delta) {
        input.mouseScrolled(state, mx, my, delta);
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return input.keyPressed(state, keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        return input.charTyped(state, chr, modifiers);
    }

    public boolean onFilesDrop(List<Path> paths) {
        return input.onFilesDrop(state, paths);
    }

    public enum Category {
        HUD("HUD", "clickgui.settings.tab.hud", "HUD", IconKind.TEXT),
        UI("UI", "clickgui.settings.tab.ui", "UI", IconKind.TEXT),
        THEMES("G", "clickgui.settings.tab.themes", "Themes", IconKind.FONT),
        MAIN_SETTINGS("columns-3-cog", "clickgui.settings.tab.main_settings", "Main Settings", IconKind.SVG),
        RELATIONS("user-pen", "clickgui.settings.tab.relations", "Relations", IconKind.SVG),
        CONFIGS("folder-cog", "clickgui.settings.tab.configs", "Configs", IconKind.SVG);

        private final String token;
        private final String titleKey;
        private final String fallbackTitle;
        private final IconKind iconKind;

        Category(String token, String titleKey, String fallbackTitle, IconKind iconKind) {
            this.token = token;
            this.titleKey = titleKey;
            this.fallbackTitle = fallbackTitle;
            this.iconKind = iconKind;
        }

        public String token() {
            return token;
        }

        public String title() {
            return ClickGuiI18n.tr(titleKey, fallbackTitle);
        }

        public boolean iconToken() {
            return iconKind == IconKind.FONT;
        }

        public boolean svgIcon() {
            return iconKind == IconKind.SVG;
        }

        public enum IconKind {
            TEXT,
            FONT,
            SVG
        }
    }
}
