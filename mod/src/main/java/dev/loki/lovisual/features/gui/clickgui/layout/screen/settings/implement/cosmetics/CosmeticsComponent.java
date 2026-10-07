package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.preview.PreviewEditor;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.preview.PreviewFileDialog;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.cosmetics.preview.PreviewStore;
import dev.loki.lovisual.features.gui.clickgui.settings.core.Setting;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiI18n;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.features.module.core.Module;
import dev.loki.lovisual.features.module.core.ModuleManager;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.nio.file.Path;
import java.util.List;

/** Cosmetics settings category: kind pills on top, a grid of cosmetic cards below. */
public final class CosmeticsComponent {
    private final PreviewStore previews = new PreviewStore();
    private final PreviewEditor editor = new PreviewEditor(previews);
    private final CosmeticsRenderer renderer = new CosmeticsRenderer(previews);
    private CosmeticKind kind = CosmeticKind.WINGS;
    private float scroll;

    public void resetScroll() {
        scroll = 0f;
    }

    public void render(float menuX, float menuY, float menuW, float menuH, float mx, float my, float scale) {
        scroll = ClickGuiMath.clamp(scroll, -renderer.maxScroll(), 0f);
        renderer.render(kind, modules(kind), menuX, menuY, menuW, menuH, scroll, scale);
        editor.render(menuX, menuY, menuW, menuH, mx, my, scale);
    }

    /** True while the picture editor covers the page and owns the input. */
    public boolean isEditing() {
        return editor.isOpen();
    }

    public void mouseReleased() {
        editor.mouseReleased();
    }

    public boolean keyPressed(int keyCode) {
        return editor.keyPressed(keyCode);
    }

    /** A picture dropped on a card opens the editor for it. */
    public boolean onFilesDrop(List<Path> paths, float mx, float my) {
        if (editor.isOpen()) return true;
        String id = renderer.cardAt(mx, my);
        if (id == null) return false;
        for (Path path : paths) {
            if (PreviewFileDialog.isPicture(path)) return editor.open(id, path);
        }
        return false;
    }

    public void scroll(double amount, float scale) {
        if (editor.mouseScrolled(amount)) return;
        scroll = ClickGuiMath.clamp(scroll + (float) amount * 18f * scale, -renderer.maxScroll(), 0f);
    }

    /** Returns true when the click was consumed; {@code onSettings} receives the module to open settings for. */
    public boolean click(float mx, float my, int button, java.util.function.Consumer<Module> onSettings) {
        if (editor.mousePressed(mx, my, button)) return true;
        String pictureCard = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? renderer.pictureButtonAt(mx, my) : null;
        if (pictureCard != null) {
            Path chosen = PreviewFileDialog.choose(ClickGuiI18n.tr("clickgui.settings.cosmetics.pick", "Choose a picture"));
            if (chosen != null) editor.open(pictureCard, chosen);
            return true;
        }
        String cropCard = button == GLFW.GLFW_MOUSE_BUTTON_LEFT ? renderer.cropButtonAt(mx, my) : null;
        if (cropCard != null) {
            editor.open(cropCard, null);
            return true;
        }
        CosmeticKind pill = renderer.pillAt(mx, my);
        if (pill != null && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            if (pill != kind) {
                kind = pill;
                scroll = 0f;
            }
            return true;
        }
        String id = renderer.cardAt(mx, my);
        Module module = id == null ? null : ModuleManager.get(id);
        if (module == null) return false;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) module.toggle();
        else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) onSettings.accept(module);
        return true;
    }

    /** Settings of a cosmetic module with their i18n resolved, or an empty list. */
    public static List<Setting> settingsOf(Module module) {
        List<Setting> settings = module.getSettings();
        if (settings == null) return List.of();
        for (Setting setting : settings) {
            if (setting != null) setting.preflightI18n();
        }
        return settings;
    }

    static List<Module> modules(CosmeticKind kind) {
        List<Module> out = new ArrayList<>();
        for (String id : kind.moduleIds()) {
            Module module = ModuleManager.get(id);
            if (module != null) out.add(module);
        }
        return out;
    }
}
