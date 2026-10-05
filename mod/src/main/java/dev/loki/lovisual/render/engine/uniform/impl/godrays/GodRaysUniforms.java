package dev.loki.lovisual.render.engine.uniform.impl.godrays;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

public enum GodRaysUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4().putVec4().putVec4().putVec4()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - GodRays UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 2;

    public static void update(float sunU, float sunV, float reach, int samples, float sampleWeight,
                              float strength, float decay, float red, float green, float blue, float fade,
                              int width, int height) {
        DATA.sunU = sunU;
        DATA.sunV = sunV;
        DATA.reach = reach;
        DATA.samples = samples;
        DATA.sampleWeight = sampleWeight;
        DATA.strength = strength;
        DATA.decay = decay;
        DATA.red = red;
        DATA.green = green;
        DATA.blue = blue;
        DATA.fade = fade;
        DATA.width = width;
        DATA.height = height;
        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private float sunU, sunV, reach, samples;
        private float sampleWeight, strength, decay;
        private float red, green, blue, fade;
        private int width, height;

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder b = Std140Builder.intoBuffer(buffer);
            b.putFloat(sunU).putFloat(sunV).putFloat(reach).putFloat(0.0f);
            b.putFloat(samples).putFloat(sampleWeight).putFloat(strength).putFloat(decay);
            b.putFloat(red).putFloat(green).putFloat(blue).putFloat(fade);
            b.putFloat(width > 0 ? 1.0f / width : 0.0f).putFloat(height > 0 ? 1.0f / height : 0.0f)
                    .putFloat(width).putFloat(height);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
