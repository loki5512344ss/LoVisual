package dev.loki.lovisual.render.engine.msaa;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import dev.loki.lovisual.mixininterface.device.IMsaaTexture;
import dev.loki.lovisual.render.engine.core.system.LoVisualRenderSystem;
import lombok.Getter;

@Getter
public final class MsaaFramebuffer extends RenderTarget {
    private int samples;

    public MsaaFramebuffer(String name, int width, int height, boolean useDepthAttachment, int samples) {
        super(name, useDepthAttachment, GpuFormat.RGBA8_UNORM);
        this.samples = samples;
        RenderSystem.assertOnRenderThread();
        this.resize(width, height);
    }

    private static int textureSamples(GpuTexture texture) {
        return texture instanceof IMsaaTexture msaa ? msaa.lovisual$getSamples() : 1;
    }

    @Override
    public void createBuffers(int width, int height) {
        RenderSystem.assertOnRenderThread();
        if (!LoVisualRenderSystem.rhi().msaa().supported()) {
            throw new IllegalStateException("MSAA device not available");
        }

        this.width = width;
        this.height = height;

        int usage = GpuTexture.USAGE_COPY_DST
                | GpuTexture.USAGE_COPY_SRC
                | GpuTexture.USAGE_TEXTURE_BINDING
                | GpuTexture.USAGE_RENDER_ATTACHMENT;

        try {
            if (this.useDepth) {
                this.depthTexture = LoVisualRenderSystem.rhi().msaa().createTexture(
                        this.label + " / Depth",
                        usage,
                        GpuFormat.D32_FLOAT,
                        width,
                        height,
                        samples
                );
                this.depthTextureView = RenderSystem.getDevice().createTextureView(this.depthTexture);
            }

            this.colorTexture = LoVisualRenderSystem.rhi().msaa().createTexture(
                    this.label + " / Color",
                    usage,
                    GpuFormat.RGBA8_UNORM,
                    width,
                    height,
                    samples
            );
            this.colorTextureView = RenderSystem.getDevice().createTextureView(this.colorTexture);

            int colorSamples = textureSamples(this.colorTexture);
            int depthSamples = this.depthTexture != null ? textureSamples(this.depthTexture) : colorSamples;
            if (colorSamples <= 1 || depthSamples <= 1 || colorSamples != depthSamples) {
                throw new IllegalStateException("Incomplete MSAA target sample layout: color="
                        + colorSamples + ", depth=" + depthSamples + ", requested=" + samples);
            }
            this.samples = colorSamples;
        } catch (RuntimeException | Error t) {
            this.destroyBuffers();
            throw t;
        }
    }
}
