package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.config;

import dev.loki.lovisual.config.profile.codec.ConfigProfileType;
import dev.loki.lovisual.config.profile.service.ConfigProfileService;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import org.lwjgl.glfw.GLFW;

import java.nio.file.Path;
import java.util.List;

public final class ConfigProfilesComponent {
    private final ConfigProfileCardComponent cardComponent = new ConfigProfileCardComponent();
    private final ConfigDiffPanelComponent diffPanelComponent = new ConfigDiffPanelComponent();
    private final ConfigProfilesState state = new ConfigProfilesState();
    private final ConfigProfilesActions actions = new ConfigProfilesActions(state, diffPanelComponent);
    private final ConfigProfilesToolbarRenderer toolbar = new ConfigProfilesToolbarRenderer(state);
    private final ConfigProfilesListRenderer listRenderer = new ConfigProfilesListRenderer(state, cardComponent);

    private static boolean inside(ConfigProfileCardComponent.ActionHit hit, float mx, float my) {
        return hit != null && ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h());
    }

    public void resetScroll() {
        state.resetScroll();
        diffPanelComponent.resetScroll();
    }

    public void render(float menuX, float menuY, float menuW, float menuH, float mx, float my, float scale) {
        state.hits().clear();
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        float areaX = menuX + 31f * scale;
        float areaY = menuY + 33f * scale;
        float areaW = menuW - 42f * scale;
        float areaH = menuH - 39f * scale;
        toolbar.render(areaX, areaY, areaW, mx, my, scale, palette);
        float listX = areaX;
        float listY = areaY + 24f * scale;
        float listW = areaW;
        float listH = areaH - 24f * scale;
        List<ConfigProfileMeta> entries = actions.filteredProfiles(ConfigProfileService.INSTANCE.list(state.type()));
        listRenderer.renderCards(listX, listY, listW, listH, entries, mx, my, scale, palette);
        if (state.selectedDiff() != null || state.diffError() != null || diffPanelComponent.isActive()) {
            diffPanelComponent.render(menuX + menuW + 24f * scale, menuY + 4f * scale, 160f * scale,
                    Math.min(200f * scale, menuH - 8f * scale), state.selected(), state.selectedDiff(), state.diffError(), scale, palette);
            if (!diffPanelComponent.isOpen() && !diffPanelComponent.isActive()) {
                state.selectedDiff(null);
                state.diffError(null);
            }
        }
        listRenderer.renderStatus(areaX, areaY, areaW, scale, palette);
    }

    public boolean mousePressedScrollbar(float mx, float my, int button) {
        if (button != 0) return false;
        if (diffPanelComponent.mousePressedScrollbar(mx, my, button)) return true;
        if (!listRenderer.isScrollbarHovered(mx, my)) return false;
        state.draggingScrollbar(true);
        state.scrollbarDragOffset(ClickGuiMath.insideRect(mx, my, state.scrollbarX(), state.scrollbarThumbY(), state.scrollbarW(), state.scrollbarThumbH())
                ? my - state.scrollbarThumbY() : state.scrollbarThumbH() * 0.5f);
        listRenderer.scrollToMouse(my);
        return true;
    }

    public void mouseReleased(int button) {
        diffPanelComponent.mouseReleased(button);
        if (button == 0) state.draggingScrollbar(false);
    }

    public void scroll(float mx, float my, float menuX, float menuY, float menuW, float menuH, double amount, float scale) {
        if (diffPanelComponent.scroll(mx, my, amount)) return;
        float x = menuX + 31f * scale;
        float y = menuY + 55f * scale;
        float w = menuW - 42f * scale;
        float h = menuH - 61f * scale;
        if (!ClickGuiMath.insideRect(mx, my, x, y, w, h)) return;
        state.scroll(state.scroll() + (float) (amount * 22f));
    }

    public boolean click(float mx, float my, int button) {
        if (button != 0) return false;
        if (state.selectedDiff() != null || state.diffError() != null) {
            ConfigDiffPanelComponent.HeaderAction diffAction = diffPanelComponent.headerAction(mx, my, button);
            if (diffAction == ConfigDiffPanelComponent.HeaderAction.CLOSE) { diffPanelComponent.close(); return true; }
            if (diffAction == ConfigDiffPanelComponent.HeaderAction.SAVE) { actions.overwriteSelected(); return true; }
        }
        if (toolbar.modulesPill().contains(mx, my)) { actions.switchType(ConfigProfileType.MODULES); return true; }
        if (toolbar.hudPill().contains(mx, my)) { actions.switchType(ConfigProfileType.HUD); return true; }
        if (toolbar.themesPill().contains(mx, my)) { actions.switchType(ConfigProfileType.THEMES); return true; }
        if (toolbar.saveButton().contains(mx, my)) { toolbar.pulseSave(); actions.saveCurrent(); return true; }
        if (listRenderer.isScrollbarHovered(mx, my)) return true;
        for (ConfigProfilesState.CardEntryHit entryHit : state.hits()) {
            var hit = entryHit.hit();
            if (inside(hit.title(), mx, my)) { actions.select(entryHit.meta()); state.beginRename(entryHit.meta(), hit.title().x(), hit.title().y(), hit.title().w(), hit.title().h()); return true; }
            if (inside(hit.diff(), mx, my)) { actions.select(entryHit.meta()); diffPanelComponent.resetScroll(); actions.loadDiff(entryHit.meta()); return true; }
            if (inside(hit.apply(), mx, my)) { actions.select(entryHit.meta()); actions.apply(entryHit.meta()); return true; }
            if (inside(hit.delete(), mx, my)) { actions.delete(entryHit.meta()); return true; }
            if (ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) { actions.select(entryHit.meta()); return true; }
        }
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!state.isRenaming()) return false;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { state.cancelRename(); return true; }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { actions.commitRename(); return true; }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (!state.editingText().isEmpty()) state.editingText(state.editingText().substring(0, state.editingText().length() - 1));
            return true;
        }
        return true;
    }

    public boolean charTyped(char chr, int modifiers) {
        if (!state.isRenaming()) return false;
        if (chr >= 32 && chr != 127 && state.editingText().length() < 48) state.editingText(state.editingText() + chr);
        return true;
    }

    public boolean importDroppedFiles(List<Path> paths) {
        return actions.importDroppedFiles(paths);
    }
}
