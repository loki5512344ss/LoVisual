package dev.loki.lovisual.render.engine.uniform.impl.hand;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;
import dev.loki.lovisual.features.module.modules.visuals.scene.visual.Chams;

import java.nio.ByteBuffer;

/**
 * Единый UBO для дополнительных режимов руки Chams (Snow/Chroma/Solid/Stripes/Glow/Balatro).
 * Неиспользуемые режимом поля просто игнорируются шейдером.
 */
public enum HandChamsUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putVec4() // u_View
            .putVec4() // u_Mix
            .putVec4() // u_ColorA
            .putVec4() // u_ColorB
            .putVec4() // u_ColorC
            .putVec4() // u_ColorD
            .putVec4() // u_Common
            .putVec4() // u_Glow
            .putVec4() // u_Flags
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - HandChams UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 1;

    public static void update(int colorA,
                              int colorB,
                              int colorC,
                              int colorD,
                              float resolutionX,
                              float resolutionY,
                              float time,
                              float effectAlpha,
                              float chromaSpeed,
                              float chromaSaturation,
                              float chromaBrightness,
                              float snowIntensity,
                              float snowSpeed,
                              float stripesWidth,
                              float stripesSpeed,
                              boolean fillGlow,
                              float glowRadius,
                              float glowPower,
                              float glowDispersion,
                              boolean hideHand) {
        putRgb(DATA.colorA, colorA);
        putRgb(DATA.colorB, colorB);
        putRgb(DATA.colorC, colorC);
        putRgb(DATA.colorD, colorD);

        DATA.view[0] = resolutionX;
        DATA.view[1] = resolutionY;
        DATA.view[2] = time;

        DATA.mix[0] = clamp01(effectAlpha);
        DATA.mix[1] = chromaSpeed;
        DATA.mix[2] = clamp01(chromaSaturation);
        DATA.mix[3] = chromaBrightness;

        DATA.common[0] = snowIntensity;
        DATA.common[1] = snowSpeed;
        DATA.common[2] = stripesWidth;
        DATA.common[3] = stripesSpeed;

        DATA.glow[0] = fillGlow ? 1.0f : 0.0f;
        DATA.glow[1] = glowRadius;
        DATA.glow[2] = glowPower;
        DATA.glow[3] = glowDispersion;

        DATA.flags[0] = hideHand ? 1.0f : 0.0f;

        LoVisualRenderSystem.uniforms().writeCached(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, 1, true, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static void putRgb(float[] out, int argb) {
        out[0] = ((argb >>> 16) & 0xFF) / 255f;
        out[1] = ((argb >>> 8) & 0xFF) / 255f;
        out[2] = (argb & 0xFF) / 255f;
        out[3] = 1.0f;
    }

    private static float clamp01(float value) {
        return Math.max(0.0f, Math.min(1.0f, value));
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final float[] view = new float[4];
        private final float[] mix = new float[4];
        private final float[] colorA = new float[4];
        private final float[] colorB = new float[4];
        private final float[] colorC = new float[4];
        private final float[] colorD = new float[4];
        private final float[] common = new float[4];
        private final float[] glow = new float[4];
        private final float[] flags = new float[4];

        @Override
        public void write(ByteBuffer buffer) {
            Std140Builder.intoBuffer(buffer)
                    .putFloat(view[0]).putFloat(view[1]).putFloat(view[2]).putFloat(view[3])
                    .putFloat(mix[0]).putFloat(mix[1]).putFloat(mix[2]).putFloat(mix[3])
                    .putFloat(colorA[0]).putFloat(colorA[1]).putFloat(colorA[2]).putFloat(colorA[3])
                    .putFloat(colorB[0]).putFloat(colorB[1]).putFloat(colorB[2]).putFloat(colorB[3])
                    .putFloat(colorC[0]).putFloat(colorC[1]).putFloat(colorC[2]).putFloat(colorC[3])
                    .putFloat(colorD[0]).putFloat(colorD[1]).putFloat(colorD[2]).putFloat(colorD[3])
                    .putFloat(common[0]).putFloat(common[1]).putFloat(common[2]).putFloat(common[3])
                    .putFloat(glow[0]).putFloat(glow[1]).putFloat(glow[2]).putFloat(glow[3])
                    .putFloat(flags[0]).putFloat(flags[1]).putFloat(flags[2]).putFloat(flags[3]);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
