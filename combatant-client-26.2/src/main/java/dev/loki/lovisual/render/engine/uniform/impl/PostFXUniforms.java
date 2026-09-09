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
import dev.loki.lovisual.features.module.modules.visuals.fx.PostFX;

public enum PostFXUniforms {
    ;
    public static final int FLOAT_COUNT = 32;
    public static final int SIZE = new Std140SizeCalculator()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - PostFX UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 16;

    public static void update(float[] values) {
        if (values == null || values.length < FLOAT_COUNT) return;
        System.arraycopy(values, 0, DATA.values, 0, FLOAT_COUNT);
        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] values = new float[FLOAT_COUNT];

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder b = Std140Builder.intoBuffer(buffer);
            for (int i = 0; i < FLOAT_COUNT; i++) {
                b.putFloat(values[i]);
            }
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

}
