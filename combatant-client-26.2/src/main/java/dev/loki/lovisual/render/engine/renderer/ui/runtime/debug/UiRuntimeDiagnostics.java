/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.renderer.ui.runtime.debug;

import dev.loki.lovisual.render.engine.renderer.ui.runtime.core.UiNode;

public final class UiRuntimeDiagnostics {
    private final UiPerfCounters counters = new UiPerfCounters();

    public UiPerfCounters counters() {
        return counters;
    }

    public String dump(UiNode root) {
        return UiTreeDumper.dump(root);
    }
}
