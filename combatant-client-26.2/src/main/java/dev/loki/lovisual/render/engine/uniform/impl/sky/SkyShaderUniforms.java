/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.uniform.impl.sky;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

public enum SkyShaderUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - SkyShader UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 4;

    public static void update(float time, float speed, float scale, float intensity,
                               float width, float height, float alpha, float fov,
                               float colorR, float colorG, float colorB,
                               float cameraDirX, float cameraDirY) {
        DATA.params0[0] = time;
        DATA.params0[1] = speed;
        DATA.params0[2] = scale;
        DATA.params0[3] = intensity;

        DATA.params1[0] = width;
        DATA.params1[1] = height;
        DATA.params1[2] = alpha;
        DATA.params1[3] = fov;

        DATA.color[0] = colorR;
        DATA.color[1] = colorG;
        DATA.color[2] = colorB;
        DATA.color[3] = 0.0f;

        DATA.cameraDir[0] = cameraDirX;
        DATA.cameraDir[1] = cameraDirY;
        DATA.cameraDir[2] = 0.0f;
        DATA.cameraDir[3] = 0.0f;

        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] params0 = new float[4];
        private final float[] params1 = new float[4];
        private final float[] color = new float[4];
        private final float[] cameraDir = new float[4];

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(params0[0]).putFloat(params0[1]).putFloat(params0[2]).putFloat(params0[3])
                    .putFloat(params1[0]).putFloat(params1[1]).putFloat(params1[2]).putFloat(params1[3])
                    .putFloat(color[0]).putFloat(color[1]).putFloat(color[2]).putFloat(color[3])
                    .putFloat(cameraDir[0]).putFloat(cameraDir[1]).putFloat(cameraDir[2]).putFloat(cameraDir[3]);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
