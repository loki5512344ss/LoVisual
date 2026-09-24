package dev.loki.lovisual.render.engine.uniform.impl.post;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

public enum PostProcessUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putFloat()
            .putFloat()
            .putFloat()
            .putFloat()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - PostProcess UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 16;

    public static void update(float strength, float desat, float contrast, float padding) {
        DATA.strength = strength;
        DATA.desat = desat;
        DATA.contrast = contrast;
        DATA.padding = padding;
        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private float strength;
        private float desat;
        private float contrast;
        private float padding;

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(strength)
                    .putFloat(desat)
                    .putFloat(contrast)
                    .putFloat(padding);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }

}
