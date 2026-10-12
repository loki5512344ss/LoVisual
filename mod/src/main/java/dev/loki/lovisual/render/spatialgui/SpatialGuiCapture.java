/*
 * Ported/adapted from Spatial GUI (https://github.com/tastytrash/Spatial-GUI), MIT License.
 * Copyright (c) tastytrash and Spatial GUI contributors.
 *
 * This file is part of LoVisual and is distributed under LoVisual's own GPLv3 license terms
 * for the modifications made here (see mod/LICENSE and mod/THIRD_PARTY_NOTICES.md); the MIT-licensed
 * portions ported from Spatial GUI remain available under the MIT terms above.
 */
package dev.loki.lovisual.render.spatialgui;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.util.logging.DebugLog;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.GuiBannerResultRenderer;
import net.minecraft.client.gui.render.pip.GuiBookModelRenderer;
import net.minecraft.client.gui.render.pip.GuiEntityRenderer;
import net.minecraft.client.gui.render.pip.GuiProfilerChartRenderer;
import net.minecraft.client.gui.render.pip.GuiSkinRenderer;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.joml.Vector4f;

import java.util.List;

/**
 * Owns the off-screen {@link RenderTarget} that the currently open {@link net.minecraft.client.gui.screens.Screen}
 * is rendered into while the SpatialGui module displays it as a 3D plane instead of a flat overlay.
 * <p>
 * Follows the reference Spatial-GUI architecture: the screen is extracted into a private
 * {@link GuiRenderState} served by a second {@link GuiRenderer} instance (see the spatialgui mixins), and that
 * instance's draw pass is redirected into {@link #target()} while the vanilla main GUI pass keeps rendering
 * the HUD untouched. Texture size matches the window framebuffer; the target is cleared to transparent once
 * per frame before the main GUI pass.
 */
public final class SpatialGuiCapture {
    private static final SpatialGuiCapture INSTANCE = new SpatialGuiCapture();

    private TextureTarget target;
    private int width;
    private int height;

    private GuiRenderState screenRenderState;
    private GuiRenderer screenGuiRenderer;

    private SpatialGuiCapture() {
    }

    public static SpatialGuiCapture get() {
        return INSTANCE;
    }

    /**
     * Ensures the capture target exists and matches the requested pixel size. Must be called on the render thread.
     */
    public RenderTarget ensure(int pixelWidth, int pixelHeight) {
        pixelWidth = Math.max(1, pixelWidth);
        pixelHeight = Math.max(1, pixelHeight);
        if (target == null) {
            target = new TextureTarget("LoVisual Spatial GUI", pixelWidth, pixelHeight, true, GpuFormat.RGBA8_UNORM);
            width = pixelWidth;
            height = pixelHeight;
        } else if (width != pixelWidth || height != pixelHeight) {
            target.resize(pixelWidth, pixelHeight);
            width = pixelWidth;
            height = pixelHeight;
        }
        return target;
    }

    /** Current capture target, created lazily against the real window size if nothing prepared it yet. */
    public TextureTarget target() {
        if (target == null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getWindow() != null) {
                ensure(mc.getWindow().getWidth(), mc.getWindow().getHeight());
            }
        }
        return target;
    }

    public void clearTransparent() {
        if (target == null) return;
        GpuTexture colorTexture = target.getColorTexture();
        if (colorTexture == null) return;
        if (target.useDepth && target.getDepthTexture() != null) {
            RenderSystem.getDevice().createCommandEncoder()
                    .clearColorAndDepthTextures(colorTexture, new Vector4f(0.0F, 0.0F, 0.0F, 0.0F), target.getDepthTexture(), 1.0D);
        } else {
            RenderSystem.getDevice().createCommandEncoder().clearColorTexture(colorTexture, new Vector4f(0.0F, 0.0F, 0.0F, 0.0F));
        }
    }

    /** Second GuiRenderer whose draw pass is redirected into the capture target; built on first use. */
    public GuiRenderer screenGuiRenderer() {
        if (screenGuiRenderer == null) {
            Minecraft mc = Minecraft.getInstance();
            screenRenderState = new GuiRenderState();
            screenGuiRenderer = new GuiRenderer(
                    screenRenderState,
                    mc.gameRenderer.featureRenderDispatcher(),
                    List.of(
                            new GuiEntityRenderer(mc.getEntityRenderDispatcher()),
                            new GuiSkinRenderer(),
                            new GuiBookModelRenderer(),
                            new GuiBannerResultRenderer(mc.getAtlasManager()),
                            new GuiProfilerChartRenderer()));
        }
        return screenGuiRenderer;
    }

    /** Extracts the screen into the isolated render state instead of the vanilla main GUI state. */
    public void extractScreen(Screen screen, int mouseX, int mouseY, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        screenGuiRenderer();
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(mc, screenRenderState, mouseX, mouseY);
        screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
        int[] count = {0};
        screenRenderState.forEachElement(e -> count[0]++, GuiRenderState.TraverseRange.ALL);
        DebugLog.renderThreadOnChange("sg.extract.elements", count[0],
                "[SpatialGui] isolated render state elements=%d screen=%s", count[0], screen.getClass().getSimpleName());
    }

    /** Builds and draws the isolated frame: prepare, execute into the capture target, reset the state. */
    public void renderIsolatedFrame() {
        GuiRenderer renderer = screenGuiRenderer();
        renderer.render();
        renderer.endFrame();
        DebugLog.renderThreadOnce("sg.isolated.render", "[SpatialGui] isolated render pass executed");
    }

    public GpuTextureView colorTextureView() {
        return target == null ? null : target.getColorTextureView();
    }

    public boolean isReady() {
        return target != null && target.getColorTextureView() != null;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void close() {
        if (target != null) {
            target.destroyBuffers();
            target = null;
        }
    }
}
