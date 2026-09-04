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

/**
 * Std140 UBO for MSDF text rendering.
 * <p>
 * Layout (std140):
 * vec4 data; // x = pxRange, y = atlasWidth, z = atlasHeight, w = unused
 */
public enum MsdfTextUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - MSDF Text UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 16;

    public static void update(float pxRange, int atlasWidth, int atlasHeight) {
        float safeW = atlasWidth <= 0 ? 1f : atlasWidth;
        float safeH = atlasHeight <= 0 ? 1f : atlasHeight;

        DATA.values[0] = pxRange;
        DATA.values[1] = safeW;
        DATA.values[2] = safeH;
        DATA.values[3] = 0f;

        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] values = new float[4];

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(values[0])
                    .putFloat(values[1])
                    .putFloat(values[2])
                    .putFloat(values[3]);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

}
