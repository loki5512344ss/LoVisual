/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.uniform.impl;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import org.joml.Matrix4f;
import dev.loki.lovisual.render.engine.core.LoVisualRenderSystem;
import dev.loki.lovisual.render.engine.rhi.uniform.LoVisualUniformAllocator;

public enum WetSurfaceUniforms {
    ;
    public static final int SIZE = new Std140SizeCalculator()
            .putMat4f()
            .putMat4f()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .putFloat().putFloat().putFloat().putFloat()
            .get();

    private static final Data DATA = new Data();
    private static final String UNIFORM_NAME = "LoVisual - WetSurface UBO";
    private static final int EXPECTED_WRITES_PER_FRAME = 2;

    public static void update(Matrix4f projection,
                              Matrix4f invView,
                              float camX, float camY, float camZ,
                              float invW, float invH, float time, boolean mainDepthEnabled,
                              float wetness, float puddleCoverage, float reflectionStrength, float rippleStrength,
                              float maxDistance, int reflectionSteps, float rainAmount, boolean ripplesEnabled) {
        DATA.projection.set(projection);
        DATA.invView.set(invView);
        DATA.camX = camX;
        DATA.camY = camY;
        DATA.camZ = camZ;
        DATA.invW = invW;
        DATA.invH = invH;
        DATA.time = time;
        DATA.mainDepthEnabled = mainDepthEnabled ? 1.0f : 0.0f;
        DATA.wetness = wetness;
        DATA.puddleCoverage = puddleCoverage;
        DATA.reflectionStrength = reflectionStrength;
        DATA.rippleStrength = rippleStrength;
        DATA.maxDistance = maxDistance;
        DATA.reflectionSteps = reflectionSteps;
        DATA.rainAmount = rainAmount;
        DATA.ripplesEnabled = ripplesEnabled ? 1.0f : 0.0f;
        LoVisualRenderSystem.uniforms().write(UNIFORM_NAME, SIZE, EXPECTED_WRITES_PER_FRAME, DATA);
    }

    public static GpuBufferSlice get() {
        return LoVisualRenderSystem.uniforms().current(UNIFORM_NAME);
    }

    private static final class Data implements LoVisualUniformAllocator.UniformWriter {
        private final Matrix4f projection = new Matrix4f();
        private final Matrix4f invView = new Matrix4f();
        private float camX, camY, camZ;
        private float invW, invH, time, mainDepthEnabled;
        private float wetness, puddleCoverage, reflectionStrength, rippleStrength;
        private float maxDistance, reflectionSteps, rainAmount, ripplesEnabled;

        @Override
        public void write(java.nio.ByteBuffer buffer) {
            Std140Builder b = Std140Builder.intoBuffer(buffer);
            b.putMat4f(projection);
            b.putMat4f(invView);
            b.putFloat(camX).putFloat(camY).putFloat(camZ).putFloat(0.0f);
            b.putFloat(invW).putFloat(invH).putFloat(time).putFloat(mainDepthEnabled);
            b.putFloat(wetness).putFloat(puddleCoverage).putFloat(reflectionStrength).putFloat(rippleStrength);
            b.putFloat(maxDistance).putFloat(reflectionSteps).putFloat(rainAmount).putFloat(ripplesEnabled);
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }
    }
}
