package dev.loki.lovisual.render.engine.command.texture;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.renderer.ui.draw.paint.UiPaint;
import dev.loki.lovisual.render.engine.renderer.ui.draw.shape.UiShape;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

public record UiTextureDrawCommand(GpuTextureView view,
                                   GpuSampler sampler,
                                   UiShape shape,
                                   UiPaint tint,
                                   float u0,
                                   float v0,
                                   float u1,
                                   float v1,
                                   boolean masked) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.TEXTURE;
    }
}
