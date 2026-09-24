
package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.relations;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;
import dev.loki.lovisual.features.module.modules.misc.DefineTarget;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Online player picker for Relations/DefineTarget.
 * Facade that delegates data/scroll/render/input to helpers in {@code picker/}.
 */
public final class OnlineRelationPlayerPickerComponent {
    Consumer<String> statusSink;
    final OnlinePickerData data = new OnlinePickerData();
    final OnlinePickerScroll scroll = new OnlinePickerScroll();
    private final OnlinePickerRenderer renderer = new OnlinePickerRenderer();
    private final OnlinePickerInput input = new OnlinePickerInput();

    boolean openTarget;
    boolean searchOpen;
    boolean searchFocused;
    DefineTarget.RelationTargetMode mode = DefineTarget.RelationTargetMode.FRIEND;

    Rect bounds = new Rect(0f, 0f, 0f, 0f);
    Rect searchButton = new Rect(0f, 0f, 0f, 0f);
    Rect searchField = new Rect(0f, 0f, 0f, 0f);
    Rect closeButton = new Rect(0f, 0f, 0f, 0f);
    Rect listRect = new Rect(0f, 0f, 0f, 0f);

    float openAnim;
    float searchAnim;
    final List<RowHit> rowHits = new ArrayList<>();

    OnlineRelationPlayerPickerComponent(Consumer<String> statusSink) { this.statusSink = statusSink; }

    OnlineRelationPlayerPickerComponent() { this.statusSink = null; }

    void setStatusSink(Consumer<String> sink) { this.statusSink = sink; }

    boolean isVisible() { return openTarget || openAnim > 0.002f; }
    boolean blocksMovementInput() { return openTarget && searchFocused; }
    boolean isOpen() { return openTarget; }
    boolean isSearchFocused() { return openTarget && searchFocused; }
    String searchText() { return data.searchText(); }
    void focusSearch() { input.focusSearch(this); }
    void clearSearch() { input.clearSearch(this); }
    void open(DefineTarget.RelationTargetMode initialMode) {
        openTarget = true; searchOpen = false;
        if (initialMode != null) mode = initialMode;
        resetScroll(); data.refreshNow();
    }
    void close() {
        openTarget = false; searchFocused = false; scroll.draggingScrollbar = false;
        if (!data.search.isEmpty()) { data.search = ""; data.invalidateFilter(); }
    }
    void toggle(DefineTarget.RelationTargetMode initialMode) { if (openTarget) close(); else open(initialMode); }
    void resetScroll() { scroll.reset(); }
    void render(float x, float y, float w, float h, float mx, float my, float scale, SettingsGuiPalette palette) { renderer.render(this, x, y, w, h, mx, my, scale, palette); }
    boolean click(float mx, float my, int button) { return input.click(this, mx, my, button); }
    boolean mousePressedScrollbar(float mx, float my, int button) { return input.mousePressedScrollbar(this, mx, my, button); }
    void mouseReleased(int button) { input.mouseReleased(this, button); }
    void scroll(float mx, float my, double amount) { input.scroll(this, mx, my, amount); }
    boolean keyPressed(int keyCode, int scanCode, int modifiers) { return input.keyPressed(this, keyCode, scanCode, modifiers); }
    boolean charTyped(char chr, int modifiers) { return input.charTyped(this, chr, modifiers); }

    record RowHit(String name, Rect rect) {}
    record Rect(float x, float y, float w, float h) {
        boolean contains(float mx, float my) { return ClickGuiMath.insideRect(mx, my, x, y, w, h); }
    }
}
