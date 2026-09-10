/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.uniform.impl.ui;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

public enum MenuBackgroundUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4()
            .putVec4()
            .putVec4()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - MenuBackground UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 16;

    public static void update(float width, float height, float time, int accentRgb, int bgRgb) {
        DATA.params[0] = width;
        DATA.params[1] = height;
        DATA.params[2] = time;
        DATA.params[3] = 0.0f;

        DATA.accent[0] = ((accentRgb >>> 16) & 0xFF) / 255f;
        DATA.accent[1] = ((accentRgb >>> 8) & 0xFF) / 255f;
        DATA.accent[2] = (accentRgb & 0xFF) / 255f;
        DATA.accent[3] = 1.0f;

        DATA.background[0] = ((bgRgb >>> 16) & 0xFF) / 255f;
        DATA.background[1] = ((bgRgb >>> 8) & 0xFF) / 255f;
        DATA.background[2] = (bgRgb & 0xFF) / 255f;
        DATA.background[3] = 1.0f;
        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] params = new float[4];
        private final float[] accent = new float[4];
        private final float[] background = new float[4];

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(params[0]).putFloat(params[1]).putFloat(params[2]).putFloat(params[3])
                    .putFloat(accent[0]).putFloat(accent[1]).putFloat(accent[2]).putFloat(accent[3])
                    .putFloat(background[0]).putFloat(background[1]).putFloat(background[2]).putFloat(background[3]);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

}
