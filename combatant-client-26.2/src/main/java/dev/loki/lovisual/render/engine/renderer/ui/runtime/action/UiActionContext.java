/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.action;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiNode;

public record UiActionContext(UiNode node, UiActionRef ref, Object event) {
}
