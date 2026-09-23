/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.reconcile.match;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNodeSpec;

public final class UiNodeMatcher {
    public boolean canReuse(UiNode node, UiNodeSpec spec) {
        return node != null && node.sameIdentity(spec);
    }
}
