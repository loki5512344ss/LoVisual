package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.module;

import dev.loki.lovisual.features.gui.clickgui.util.ClickGuiMath;

final class ModuleCardInput {
    private final ModuleCardState state;
    private final ModuleCardLayout layout;

    ModuleCardInput(ModuleCardState state, ModuleCardLayout layout) {
        this.state = state;
        this.layout = layout;
    }

    void scroll(float mx, float my, float menuX, float menuY, float menuW, float menuH, double amount, float scale) {
        float hitX = menuX + 35f * scale;
        float hitY = menuY + 13f * scale;
        float hitW = menuW - 35f * scale + 7f * scale;
        float hitH = menuH - 13f * scale + 15f * scale;
        if (!ClickGuiMath.insideRect(mx, my, hitX, hitY, hitW, hitH)) return;
        state.scroll += (float) (amount * 20f);
    }

    boolean mousePressedScrollbar(float mx, float my, int button) {
        if (button != 0) return false;
        if (!layout.isScrollbarHovered(mx, my)) return false;
        state.draggingScrollbar = true;
        state.scrollbarDragOffset = ClickGuiMath.insideRect(mx, my, state.scrollbarX, state.scrollbarThumbY,
                state.scrollbarW, state.scrollbarThumbH)
                ? my - state.scrollbarThumbY
                : state.scrollbarThumbH * 0.5f;
        layout.scrollToMouse(my);
        return true;
    }

    void mouseReleased(int button) {
        if (button == 0) {
            state.draggingScrollbar = false;
        }
    }

    ModuleComponent.CardAction click(float mx, float my, int button) {
        if (layout.isScrollbarHovered(mx, my)) return null;
        for (CardHit hit : state.hits) {
            if (!ClickGuiMath.insideRect(mx, my, hit.x(), hit.y(), hit.w(), hit.h())) continue;
            if (button == 1 && !hit.hasSettings()) continue;
            return new ModuleComponent.CardAction(hit.getId(), button == 0, button == 1,
                    hit.statusHit(mx, my), hit.bindHit(mx, my), hit.toggleable());
        }
        return null;
    }
}
