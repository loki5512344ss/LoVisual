/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.command.core;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

/**
 * Normalized UI command root.
 * <p>
 * Renderer2D facade methods and the newer shape/effect/text/item command families can
 * coexist while callers move toward explicit UI command boundaries.
 */
public interface UiCommand {
    UiCommandKind kind();
}
