package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import java.util.List;

public final class ModuleComponent {
    private final ModuleCardState state;
    private final ModuleCardLayout layout;
    private final ModuleCardRenderer renderer;
    private final ModuleCardInput input;

    public ModuleComponent() {
        this.state = new ModuleCardState();
        this.layout = new ModuleCardLayout(state);
        this.renderer = new ModuleCardRenderer(state, layout);
        this.input = new ModuleCardInput(state, layout);
    }

    public void resetScroll() {
        state.resetScroll();
    }

    public void scroll(float mx, float my, float menuX, float menuY, float menuW, float menuH, double amount, float scale) {
        input.scroll(mx, my, menuX, menuY, menuW, menuH, amount, scale);
    }

    public void render(float menuX, float menuY, float menuW, float menuH, List<CardEntry> entries, float scale) {
        renderer.render(menuX, menuY, menuW, menuH, entries, scale);
    }

    public boolean mousePressedScrollbar(float mx, float my, int button) {
        return input.mousePressedScrollbar(mx, my, button);
    }

    public void mouseReleased(int button) {
        input.mouseReleased(button);
    }

    public CardAction click(float mx, float my, int button) {
        return input.click(mx, my, button);
    }

    public record CardEntry(String id,
                            String title,
                            String description,
                            String bindLabel,
                            boolean hasSettings,
                            boolean enabled,
                            boolean toggleable,
                            boolean shownInModuleList,
                            List<String> searchAliases) {
        public CardEntry {
            searchAliases = searchAliases == null ? List.of() : List.copyOf(searchAliases);
        }

        public CardEntry(String id,
                         String title,
                         String description,
                         String bindLabel,
                         boolean hasSettings,
                         boolean enabled,
                         boolean toggleable,
                         boolean shownInModuleList) {
            this(id, title, description, bindLabel, hasSettings, enabled, toggleable, shownInModuleList, List.of());
        }

        public CardEntry(String id,
                         String title,
                         String description,
                         String bindLabel,
                         boolean hasSettings,
                         boolean enabled,
                         boolean toggleable) {
            this(id, title, description, bindLabel, hasSettings, enabled, toggleable, true, List.of());
        }

        public String getId() {
            return id;
        }
    }

    public record CardAction(String id,
                             boolean leftClick,
                             boolean rightClick,
                             boolean statusClick,
                             boolean bindClick,
                             boolean toggleable) {
        public String getId() {
            return id;
        }
    }
}
