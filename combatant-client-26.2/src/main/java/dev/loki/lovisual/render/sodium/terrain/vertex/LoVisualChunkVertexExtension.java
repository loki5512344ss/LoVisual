/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.sodium.terrain.vertex;

public interface LoVisualChunkVertexExtension {
    public void lovisual$setSurfaceFlags(int surfaceFlags);

    public int lovisual$getSurfaceFlags();

    public void lovisual$copyData(LoVisualChunkVertexExtension dest);
}
