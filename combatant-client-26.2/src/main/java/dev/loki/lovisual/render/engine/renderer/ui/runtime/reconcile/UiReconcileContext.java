/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.reconcile;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.runtime.UiLifecycle;

public final class UiReconcileContext {
    private final UiLifecycle lifecycle;
    private final UiDiffStats stats = new UiDiffStats();

    public UiReconcileContext(UiLifecycle lifecycle) {
        this.lifecycle = lifecycle != null ? lifecycle : new UiLifecycle() {
        };
    }

    public UiLifecycle lifecycle() {
        return lifecycle;
    }

    public UiDiffStats stats() {
        return stats;
    }
}
