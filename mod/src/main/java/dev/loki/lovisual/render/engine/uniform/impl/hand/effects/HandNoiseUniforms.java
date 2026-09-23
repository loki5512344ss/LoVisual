/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.uniform.impl.hand.effects;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

public enum HandNoiseUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putFloat().putFloat().putFloat().putFloat() // u_Color
            .putFloat().putFloat().putFloat().putFloat() // u_Glow
            .putFloat().putFloat().putFloat().putFloat() // u_Params0
            .putFloat().putFloat().putFloat().putFloat() // u_Params1
            .putFloat().putFloat().putFloat().putFloat() // u_Params2
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - Hand Noise UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 16;

    public static void update(
            float colorR, float colorG, float colorB, float colorA,
            float glowR, float glowG, float glowB, float glowA,
            float edgeWidth, float quality, float octaves, float time,
            float resX, float resY, float scale, float speed,
            float contrast, float breathe, float glowStrength, float density
    ) {
        DATA.colorR = colorR;
        DATA.colorG = colorG;
        DATA.colorB = colorB;
        DATA.colorA = colorA;
        DATA.glowR = glowR;
        DATA.glowG = glowG;
        DATA.glowB = glowB;
        DATA.glowA = glowA;
        DATA.edgeWidth = edgeWidth;
        DATA.quality = quality;
        DATA.octaves = octaves;
        DATA.time = time;
        DATA.resX = resX;
        DATA.resY = resY;
        DATA.scale = scale;
        DATA.speed = speed;
        DATA.contrast = contrast;
        DATA.breathe = breathe;
        DATA.glowStrength = glowStrength;
        DATA.density = density;
        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private float colorR, colorG, colorB, colorA;
        private float glowR, glowG, glowB, glowA;
        private float edgeWidth, quality, octaves, time;
        private float resX, resY, scale, speed;
        private float contrast, breathe, glowStrength, density;

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(colorR).putFloat(colorG).putFloat(colorB).putFloat(colorA)
                    .putFloat(glowR).putFloat(glowG).putFloat(glowB).putFloat(glowA)
                    .putFloat(edgeWidth).putFloat(quality).putFloat(octaves).putFloat(time)
                    .putFloat(resX).putFloat(resY).putFloat(scale).putFloat(speed)
                    .putFloat(contrast).putFloat(breathe).putFloat(glowStrength).putFloat(density);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

}
