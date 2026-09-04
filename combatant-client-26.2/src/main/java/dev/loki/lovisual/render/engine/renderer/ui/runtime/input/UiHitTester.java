/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.input;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiBounds;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiNode;

import java.util.List;

public final class UiHitTester {
    private static boolean contains(UiBounds bounds, float x, float y) {
        return x >= bounds.x()
                && x <= bounds.x() + bounds.width()
                && y >= bounds.y()
                && y <= bounds.y() + bounds.height();
    }

    public UiHitResult hitTest(UiNode root, float x, float y) {
        UiNode hit = hitNode(root, x, y);
        if (hit == null) return UiHitResult.MISS;
        UiBounds bounds = hit.bounds();
        return new UiHitResult(hit, x - bounds.x(), y - bounds.y());
    }

    private UiNode hitNode(UiNode node, float x, float y) {
        if (node == null || !node.state().visible() || node.state().disabled()) return null;
        UiBounds bounds = node.bounds();
        if (!contains(bounds, x, y)) return null;

        List<UiNode> children = node.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            UiNode childHit = hitNode(children.get(i), x, y);
            if (childHit != null) return childHit;
        }
        return node;
    }
}
