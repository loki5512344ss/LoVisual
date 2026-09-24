package dev.loki.lovisual.render.engine.uniform.impl.sky.postprocess;

import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

/** Uniform buffer for the SkyShader "Starry" fullscreen mode (star field + milky way + procedural round moon). */
public enum SkyShaderStarryUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4().putVec4().putVec4().putVec4().putVec4()
            .putVec4().putVec4()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - SkyShaderStarry UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 4;

    public static void update(float width, float height, float time, float speed,
                               float intensity, float alpha, float fov,
                               float qx, float qy, float qz, float qw,
                               float moonDirX, float moonDirY, float moonDirZ, float moonSize,
                               float moonPhase01, float milkyWayIntensity, float starDensity,
                               float moonR, float moonG, float moonB,
                               float starR, float starG, float starB) {
        DATA.resolution[0] = width;
        DATA.resolution[1] = height;
        DATA.resolution[2] = time;
        DATA.resolution[3] = speed;

        DATA.settings[0] = intensity;
        DATA.settings[1] = alpha;
        DATA.settings[2] = fov;
        DATA.settings[3] = starDensity;

        DATA.cameraDir[0] = qx;
        DATA.cameraDir[1] = qy;
        DATA.cameraDir[2] = qz;
        DATA.cameraDir[3] = qw;

        DATA.moon[0] = moonDirX;
        DATA.moon[1] = moonDirY;
        DATA.moon[2] = moonDirZ;
        DATA.moon[3] = moonSize;

        DATA.moonExtra[0] = moonPhase01;
        DATA.moonExtra[1] = milkyWayIntensity;
        DATA.moonExtra[2] = 0f;
        DATA.moonExtra[3] = 0f;

        DATA.moonColor[0] = moonR;
        DATA.moonColor[1] = moonG;
        DATA.moonColor[2] = moonB;
        DATA.moonColor[3] = 1f;

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
        private final float[] moon = new float[4];
        private final float[] moonExtra = new float[4];
        private final float[] moonColor = new float[4];
        private final float[] starColor = new float[4];

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder builder = Std140Builder.intoBuffer(buffer);
            putVec4(builder, resolution);
            putVec4(builder, settings);
            putVec4(builder, cameraDir);
            putVec4(builder, moon);
            putVec4(builder, moonExtra);
            putVec4(builder, moonColor);
            putVec4(builder, starColor);
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
