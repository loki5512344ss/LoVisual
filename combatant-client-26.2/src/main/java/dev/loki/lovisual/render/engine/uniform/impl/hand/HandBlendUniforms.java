package dev.loki.lovisual.render.engine.uniform.impl.hand;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

import java.nio.ByteBuffer;

/**
 * UBO для dual-shader режима руки: смешивание двух шейдеров.
 */
public enum HandBlendUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - HandBlend UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 1;

    public static void update(int blendMode, float blendIntensity, float shader1Strength, float shader2Strength) {
        DATA.blend[0] = blendMode;
        DATA.blend[1] = Math.max(0.0f, Math.min(1.0f, blendIntensity));
        DATA.blend[2] = Math.max(0.0f, Math.min(1.0f, shader1Strength));
        DATA.blend[3] = Math.max(0.0f, Math.min(1.0f, shader2Strength));
        LoVisualRenderSystem.uniforms().writeCached(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, 1, true, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] blend = new float[4];

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(blend[0]).putFloat(blend[1]).putFloat(blend[2]).putFloat(blend[3]);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
