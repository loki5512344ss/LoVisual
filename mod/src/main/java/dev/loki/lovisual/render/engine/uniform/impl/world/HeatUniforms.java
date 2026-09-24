package dev.loki.lovisual.render.engine.uniform.impl.world;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

public enum HeatUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putFloat()
            .putFloat()
            .putFloat()
            .putFloat()
            .putFloat()
            .putFloat()
            .putFloat()
            .putFloat()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - HeatFX UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 16;

    public static void update(float intensity, float distortion, float scale, float speed,
                              float vignetteStrength, float vignetteRadius, float vignetteSoftness, float time) {
        DATA.intensity = intensity;
        DATA.distortion = distortion;
        DATA.scale = scale;
        DATA.speed = speed;
        DATA.vignetteStrength = vignetteStrength;
        DATA.vignetteRadius = vignetteRadius;
        DATA.vignetteSoftness = vignetteSoftness;
        DATA.time = time;
        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private float intensity;
        private float distortion;
        private float scale;
        private float speed;
        private float vignetteStrength;
        private float vignetteRadius;
        private float vignetteSoftness;
        private float time;

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(intensity)
                    .putFloat(distortion)
                    .putFloat(scale)
                    .putFloat(speed)
                    .putFloat(vignetteStrength)
                    .putFloat(vignetteRadius)
                    .putFloat(vignetteSoftness)
                    .putFloat(time);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

}
