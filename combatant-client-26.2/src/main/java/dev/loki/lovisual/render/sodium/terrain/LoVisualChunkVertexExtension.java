/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.sodium.terrain;

public interface LoVisualChunkVertexExtension {
    void lovisual$setSurfaceFlags(int surfaceFlags);

    int lovisual$getSurfaceFlags();

    void lovisual$copyData(LoVisualChunkVertexExtension dest);
}
