/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.uniform.impl.world;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;
import dev.loki.lovisual.features.module.modules.visuals.effects.hits.KillEffect;

/** Uniform buffer for the KillEffect "Portal"/"Tornado" world-space beam shaders. */
public enum KillEffectBeamUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4()
            .putVec4()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - KillEffectBeam UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 8;

    public static void update(float time, float progress, float intensity, float size,
                               float colorR, float colorG, float colorB, float colorA) {
        DATA.params0[0] = time;
        DATA.params0[1] = progress;
        DATA.params0[2] = intensity;
        DATA.params0[3] = size;

        DATA.color[0] = colorR;
        DATA.color[1] = colorG;
        DATA.color[2] = colorB;
        DATA.color[3] = colorA;

        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] params0 = new float[4];
        private final float[] color = new float[4];

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(params0[0]).putFloat(params0[1]).putFloat(params0[2]).putFloat(params0[3])
                    .putFloat(color[0]).putFloat(color[1]).putFloat(color[2]).putFloat(color[3]);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
