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

package dev.loki.lovisual.mixins;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import dev.loki.lovisual.mixininterface.IRenderPipeline;
import dev.loki.lovisual.render.engine.rhi.clip.ShapeClipRenderPassContract;

@Mixin(RenderPipeline.class)
public abstract class RenderPipelineMixin implements IRenderPipeline {
    @Unique
    private boolean lovisual$lineSmooth;

    @Unique
    private ShapeClipRenderPassContract lovisual$shapeClipContract = ShapeClipRenderPassContract.NONE;

    @Override
    public void lovisual$setLineSmooth(boolean lineSmooth) {
        this.lovisual$lineSmooth = lineSmooth;
    }

    @Override
    public boolean lovisual$getLineSmooth() {
        return lovisual$lineSmooth;
    }

    @Override
    public void lovisual$setShapeClipContract(ShapeClipRenderPassContract contract) {
        this.lovisual$shapeClipContract = contract == null ? ShapeClipRenderPassContract.NONE : contract;
    }

    @Override
    public ShapeClipRenderPassContract lovisual$getShapeClipContract() {
        return lovisual$shapeClipContract;
    }
}
