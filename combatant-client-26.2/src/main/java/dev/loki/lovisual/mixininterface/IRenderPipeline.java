/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.mixininterface;

import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;

public interface IRenderPipeline {
    void lovisual$setLineSmooth(boolean lineSmooth);

    boolean lovisual$getLineSmooth();

    void lovisual$setShapeClipContract(ShapeClipRenderPassContract contract);

    ShapeClipRenderPassContract lovisual$getShapeClipContract();
}
