/*
 * This file is part of the LoVisual Client distribution.
 * LoVisual modifications copyright (c) 2026 pivosos2007.
 *
 * Portions of this file are based on, adapted from, or implemented
 * with reference to Meteor Client
 * (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 *
 * Licensed under the GNU General Public License v3.0.
 * See THIRD_PARTY_NOTICES.md for details.
 */

package dev.loki.lovisual.mixininterface;

import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;

public interface IRenderPipeline {
    void lovisual$setLineSmooth(boolean lineSmooth);

    boolean lovisual$getLineSmooth();

    void lovisual$setShapeClipContract(ShapeClipRenderPassContract contract);

    ShapeClipRenderPassContract lovisual$getShapeClipContract();
}
