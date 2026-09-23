/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.implement.other;

import dev.loki.lovisual.features.gui.clickgui.render.ClickGuiRenderer;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.SettingsGuiPalette;
import dev.loki.lovisual.features.gui.clickgui.layout.screen.settings.render.LayoutRender2D;

public final class BackgroundComponent {

    public void render(float x, float y, float w, float h, float scale) {
        SettingsGuiPalette palette = SettingsGuiPalette.current();
        ClickGuiRenderer.drawBlur(x, y, w, h, 8f * scale, 0xFF000000, 200f / 255f);
        LayoutRender2D.roundedQuad(
                x, y, w, h, 8f * scale,
                palette.menuWindowBgLeft(),
                palette.menuWindowBgRight(),
                palette.menuWindowBgRight(),
                palette.menuWindowBgLeft()
        );
        LayoutRender2D.roundedStroke(x, y, w, h, 8f * scale, 0.1f * scale, palette.menuWindowStroke());
    }
}
