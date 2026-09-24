package dev.loki.lovisual.render.engine.uniform.impl.sky.postprocess;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

/** Uniform buffer for the SkyShader "Nebula" fullscreen mode (domain-warped fbm sky). */
public enum SkyShaderNebulaUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4().putVec4().putVec4().putVec4().putVec4()
            .putVec4().putVec4().putVec4().putVec4().putVec4()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - SkyShaderNebula UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 4;

    public static void update(float width, float height, float time, float speed,
                               float intensity, float alpha, float fov,
                               float qx, float qy, float qz, float qw,
                               float neb1R, float neb1G, float neb1B,
                               float neb2R, float neb2G, float neb2B,
                               float starR, float starG, float starB) {
        DATA.resolution[0] = width;
        DATA.resolution[1] = height;
        DATA.resolution[2] = time;
        DATA.resolution[3] = speed;

        DATA.settings[0] = intensity;
        DATA.settings[1] = alpha;
        DATA.settings[2] = fov;
        DATA.settings[3] = 0f;

        DATA.cameraDir[0] = qx;
        DATA.cameraDir[1] = qy;
        DATA.cameraDir[2] = qz;
        DATA.cameraDir[3] = qw;

        DATA.nebColor1[0] = neb1R;
        DATA.nebColor1[1] = neb1G;
        DATA.nebColor1[2] = neb1B;
        DATA.nebColor1[3] = 1f;

        DATA.nebColor2[0] = neb2R;
        DATA.nebColor2[1] = neb2G;
        DATA.nebColor2[2] = neb2B;
        DATA.nebColor2[3] = 1f;

        DATA.starColor[0] = starR;
        DATA.starColor[1] = starG;
        DATA.starColor[2] = starB;
        DATA.starColor[3] = 1f;

        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] resolution = new float[4];
        private final float[] settings = new float[4];
        private final float[] cameraDir = new float[4];
        private final float[] zero = new float[4];
        private final float[] nebColor1 = new float[4];
        private final float[] nebColor2 = new float[4];
        private final float[] starColor = new float[4];

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder builder = Std140Builder.intoBuffer(buffer);
            putVec4(builder, resolution);
            putVec4(builder, settings);
            putVec4(builder, cameraDir);
            putVec4(builder, zero); // SkyZenith (unused by shader, reserved)
            putVec4(builder, zero); // SkyHorizon (unused by shader, reserved)
            putVec4(builder, nebColor1);
            putVec4(builder, nebColor2);
            putVec4(builder, starColor);
            putVec4(builder, zero); // FogColor (unused by shader, reserved)
            putVec4(builder, zero); // FogRange (unused by shader, reserved)
        }

        private static void putVec4(Std140Builder builder, float[] v) {
            builder.putFloat(v[0]).putFloat(v[1]).putFloat(v[2]).putFloat(v[3]);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
