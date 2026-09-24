package dev.loki.lovisual.api.v0.render.callback;
import dev.loki.lovisual.api.v0.render.LoVisualRenderStage;


import com.mojang.blaze3d.vertex.PoseStack;
import dev.loki.lovisual.features.module.phase.HudPhase;
import dev.loki.lovisual.features.module.phase.WorldPhase;
import dev.loki.lovisual.render.engine.renderer.Renderer2D;
import dev.loki.lovisual.render.engine.renderer.Renderer3D;
import dev.loki.lovisual.render.engine.text.render.TextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record LoVisualRenderContext(
        String addonId,
        String callbackId,
        LoVisualRenderStage stage,
        HudPhase hudPhase,
        WorldPhase worldPhase,
        Renderer2D renderer2D,
        Renderer3D renderer3D,
        Renderer3D depthRenderer3D,
        TextRenderer textRenderer,
        GuiGraphicsExtractor guiContext,
        PoseStack poseStack,
        float tickDelta
) {
    public Minecraft client() {
        return Minecraft.getInstance();
    }
}
