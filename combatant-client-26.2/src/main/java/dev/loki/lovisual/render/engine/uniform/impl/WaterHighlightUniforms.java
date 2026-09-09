/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.uniform.impl;

import dev.loki.lovisual.render.engine.core.LoVisualRenderSystem;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;
import dev.loki.lovisual.features.module.modules.visuals.scene.BlockHighlight;

/** Uniform buffer for BlockHighlight's shader-based outline styles (WaterCaustic/ScanLines/Hologram). */
public enum WaterHighlightUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4()
            .putVec4()
            .putFloat()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - WaterHighlight UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 6;

    public static void update(float colorR, float colorG, float colorB, float colorA,
                               float patternR, float patternG, float patternB, float patternA,
                               float time) {
        DATA.color[0] = colorR;
        DATA.color[1] = colorG;
        DATA.color[2] = colorB;
        DATA.color[3] = colorA;

        DATA.pattern[0] = patternR;
        DATA.pattern[1] = patternG;
        DATA.pattern[2] = patternB;
        DATA.pattern[3] = patternA;

        DATA.time = time;

        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] color = new float[4];
        private final float[] pattern = new float[4];
        private float time;

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(color[0]).putFloat(color[1]).putFloat(color[2]).putFloat(color[3])
                    .putFloat(pattern[0]).putFloat(pattern[1]).putFloat(pattern[2]).putFloat(pattern[3])
                    .putFloat(time);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
