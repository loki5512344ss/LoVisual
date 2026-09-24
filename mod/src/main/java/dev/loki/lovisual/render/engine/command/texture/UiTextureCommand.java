package dev.loki.lovisual.render.engine.command.texture;

import com.mojang.blaze3d.textures.GpuTextureView;
import dev.loki.lovisual.render.engine.command.core.UiCommand;
import dev.loki.lovisual.render.engine.command.core.UiCommandKind;

public record UiTextureCommand(GpuTextureView texture, float x, float y, float width, float height,
                               int argb) implements UiCommand {
    @Override
    public UiCommandKind kind() {
        return UiCommandKind.TEXTURE;
    }
}
