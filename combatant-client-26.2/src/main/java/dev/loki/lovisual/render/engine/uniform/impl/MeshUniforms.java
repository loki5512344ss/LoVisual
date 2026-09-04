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
import org.joml.Matrix4f;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

public enum MeshUniforms {
    ;

    public static final int SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putMat4f()
            .putVec4()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - Mesh UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 16;

    public static void update(Matrix4f proj, Matrix4f modelView) {
        update(proj, modelView, 1.0f, 1.0f);
    }

    public static void update(Matrix4f proj, Matrix4f modelView, float viewportWidth, float viewportHeight) {
        DATA.proj = proj;
        DATA.modelView = modelView;
        DATA.viewportWidth = Math.max(1.0f, viewportWidth);
        DATA.viewportHeight = Math.max(1.0f, viewportHeight);
        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private Matrix4f proj;
        private Matrix4f modelView;
        private float viewportWidth = 1.0f;
        private float viewportHeight = 1.0f;

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putMat4f(proj)
                    .putMat4f(modelView)
                    .putFloat(viewportWidth)
                    .putFloat(viewportHeight)
                    .putFloat(1.0f / Math.max(1.0f, viewportWidth))
                    .putFloat(1.0f / Math.max(1.0f, viewportHeight));
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

}
