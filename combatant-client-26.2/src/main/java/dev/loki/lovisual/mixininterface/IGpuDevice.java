/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixininterface;

public interface IGpuDevice {
    void lovisual$pushScissor(int x, int y, int width, int height);

    void lovisual$popScissor();
}
