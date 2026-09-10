/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.resource;

public enum MeshOwnership {
    /**
     * Backed by backend-owned ring buffers. Do not close from draw code.
     */
    BACKEND_FRAME,
    /**
     * Backed by persistent resource manager object. Do not close from draw code.
     */
    PERSISTENT,
    /**
     * Externally owned legacy buffers. Do not close from draw code.
     */
    EXTERNAL,
    /**
     * Emergency immediate fallback. The handle owns these buffers and must close them after draw/frame.
     */
    TEMPORARY_OWNED
}
