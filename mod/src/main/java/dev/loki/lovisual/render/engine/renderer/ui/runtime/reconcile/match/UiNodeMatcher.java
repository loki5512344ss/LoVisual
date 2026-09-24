package dev.loki.lovisual.render.engine.renderer.ui.runtime.reconcile.match;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNode;
import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.node.UiNodeSpec;

public final class UiNodeMatcher {
    public boolean canReuse(UiNode node, UiNodeSpec spec) {
        return node != null && node.sameIdentity(spec);
    }
}
