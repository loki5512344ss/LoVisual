/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.rhi.uniform;
import dev.loki.lovisual.render.engine.rhi.uniform.FrameResource;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

/**
 * Adapter for old DynamicUniformStorage owners while they are migrated into LoVisualUniformAllocator.
 */
public final class LegacyUniformFrameResource implements FrameResource {
    private final String name;
    private final Runnable flip;
    private boolean used = true;
    private boolean lazyTrackingEnabled;

    public LegacyUniformFrameResource(String name, Runnable flip) {
        this.name = name;
        this.flip = flip;
    }

    public void markUsed() {
        used = true;
        lazyTrackingEnabled = true;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean usedThisFrame() {
        return !lazyTrackingEnabled || used;
    }

    @Override
    public void onFramePresented() {
        if (lazyTrackingEnabled && !used) return;
        flip.run();
        used = false;
    }
}
