/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.other;

import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.MenuScreen;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.category.CategoryComponent;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;

import java.util.ArrayList;
import java.util.List;

public final class CategoryContainerComponent {
    private final List<CategoryComponent> categoryComponents = new ArrayList<>();

    public CategoryContainerComponent() {
        for (MenuScreen.Category category : MenuScreen.Category.values()) {
            categoryComponents.add(new CategoryComponent(category));
        }
    }

    public void render(float x, float y, float mx, float my, MenuScreen.Category selected, float scale) {
        float offset = 0f;
        for (CategoryComponent component : categoryComponents) {
            component.render(x + 6f * scale, y + offset, mx, my, selected, scale);
            offset += 29f * scale;
            if (component.category() == MenuScreen.Category.THEMES) {
                renderSeparator(x, y + offset + 5f * scale, scale);
                offset += 10f * scale;
            } else if (component.category() == MenuScreen.Category.RELATIONS) {
                renderSeparator(x, y + offset - 4f * scale, scale);
                offset += 10f * scale;
            }
        }
    }

    public MenuScreen.Category click(float x, float y, float mx, float my, float scale) {
        float offset = 0f;
        for (CategoryComponent component : categoryComponents) {
            if (component.click(x + 6f * scale, y + offset, mx, my, scale)) {
                return component.category();
            }
            offset += 29f * scale;
            if (component.category() == MenuScreen.Category.THEMES
                    || component.category() == MenuScreen.Category.RELATIONS) {
                offset += 10f * scale;
            }
        }
        return null;
    }

    private void renderSeparator(float x, float y, float scale) {
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        float sx = x + 8f * scale;
        float sy = y;
        float sw = 26f * scale;
        float sh = 0.5f * scale;
        LayoutRender2D.rectQuad(
                sx,
                sy,
                sw,
                sh,
                LayoutRender2D.alpha(palette.menuLineLow(), 0.65f),
                LayoutRender2D.alpha(palette.menuLineStrong(), 0.72f),
                LayoutRender2D.alpha(palette.menuLineStrong(), 0.72f),
                LayoutRender2D.alpha(palette.menuLineLow(), 0.65f)
        );
    }
}
