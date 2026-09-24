package dev.loki.lovisual.render.engine.rhi.resource.atlas;

import dev.loki.lovisual.render.engine.texture.Texture;

public record GlyphAtlasPage(String id, Texture texture, int width, int height, boolean msdf) {
}
