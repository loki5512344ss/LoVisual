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
import dev.loki.lovisual.render.engine.core.context.ViewportContext;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

/**
 * Std140 UBO for batched 2D UI shaders.
 * <p>
 * Layout (std140):
 * vec4 screen; // xy = framebuffer size, zw = logical size
 */
public enum UIBatchUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - UI Batch UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 16;

    public static void update(float framebufferW, float framebufferH) {
        float safeW = framebufferW <= 0f ? 1f : framebufferW;
        float safeH = framebufferH <= 0f ? 1f : framebufferH;

        float uiScale = ViewportContext.getUiScale();
        if (uiScale <= 0f) uiScale = 1f;

        float logicalW = safeW / uiScale;
        float logicalH = safeH / uiScale;

        DATA.screen[0] = framebufferW;
        DATA.screen[1] = framebufferH;
        DATA.screen[2] = logicalW;
        DATA.screen[3] = logicalH;

        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] screen = new float[4];

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(screen[0]).putFloat(screen[1]).putFloat(screen[2]).putFloat(screen[3]);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

}
