/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.draw.edge;
import dev.loki.lovisual.render.engine.renderer.ui.draw.effect.UiPathCap;
import dev.loki.lovisual.render.engine.renderer.ui.draw.effect.UiPathJoin;

public record UiStroke(float thickness, UiPathCap cap, UiPathJoin join) {
    public static final UiStroke NONE = new UiStroke(0f, UiPathCap.BUTT, UiPathJoin.MITER);

    public static UiStroke of(double thickness) {
        return new UiStroke((float) Math.max(0.0, thickness), UiPathCap.BUTT, UiPathJoin.MITER);
    }

    public UiStroke withRoundCapsAndJoins() {
        return new UiStroke(thickness, UiPathCap.ROUND, UiPathJoin.ROUND);
    }

    public boolean enabled() {
        return thickness > 0f;
    }
}
