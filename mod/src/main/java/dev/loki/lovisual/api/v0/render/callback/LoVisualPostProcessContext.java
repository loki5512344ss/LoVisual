package dev.loki.lovisual.api.v0.render.callback;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.postprocess.post.PostProcessPass;
import net.minecraft.client.Minecraft;

public record LoVisualPostProcessContext(
        String addonId,
        String passId,
        PostProcessPass.Phase phase,
        GpuTextureView source,
        GpuTextureView destination,
        float tickDelta
) {
    public Minecraft client() {
        return Minecraft.getInstance();
    }
}
