package dev.loki.lovisual.render.engine.uniform.impl.sky;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;
import dev.loki.lovisual.features.module.modules.visuals.weather.ShaderSky;

import java.nio.ByteBuffer;

/**
 * UBO для модуля ShaderSky: общий набор параметров всех режимов неба.
 */
public enum ShaderSkyUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4() // u_Color
            .putVec4() // u_Params
            .putVec4() // u_View
            .putVec4() // u_Cam
            .putVec4() // u_Thunder
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - ShaderSky UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 1;

    public static void update(int colorRgb,
                              float alpha,
                              float time,
                              float speed,
                              float scale,
                              float intensity,
                              float resolutionX,
                              float resolutionY,
                              float yawRad,
                              float pitchRad,
                              float fov,
                              float thunderInterval,
                              float thunderChance,
                              float thunderGlow) {
        DATA.color[0] = ((colorRgb >>> 16) & 0xFF) / 255f;
        DATA.color[1] = ((colorRgb >>> 8) & 0xFF) / 255f;
        DATA.color[2] = (colorRgb & 0xFF) / 255f;
        DATA.color[3] = clamp01(alpha);

        DATA.params[0] = time;
        DATA.params[1] = speed;
        DATA.params[2] = Math.max(0.05f, scale);
        DATA.params[3] = intensity;

        DATA.view[0] = resolutionX;
        DATA.view[1] = resolutionY;
        DATA.view[2] = fov;

        DATA.cam[0] = yawRad;
        DATA.cam[1] = pitchRad;

        DATA.thunder[0] = Math.max(1.0f, thunderInterval);
        DATA.thunder[1] = clamp01(thunderChance);
        DATA.thunder[2] = thunderGlow;

        LoVisualRenderSystem.uniforms().writeCached(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, 1, true, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] color = new float[4];
        private final float[] params = new float[4];
        private final float[] view = new float[4];
        private final float[] cam = new float[4];
        private final float[] thunder = new float[4];

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(color[0]).putFloat(color[1]).putFloat(color[2]).putFloat(color[3])
                    .putFloat(params[0]).putFloat(params[1]).putFloat(params[2]).putFloat(params[3])
                    .putFloat(view[0]).putFloat(view[1]).putFloat(view[2]).putFloat(view[3])
                    .putFloat(cam[0]).putFloat(cam[1]).putFloat(cam[2]).putFloat(cam[3])
                    .putFloat(thunder[0]).putFloat(thunder[1]).putFloat(thunder[2]).putFloat(thunder[3]);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
