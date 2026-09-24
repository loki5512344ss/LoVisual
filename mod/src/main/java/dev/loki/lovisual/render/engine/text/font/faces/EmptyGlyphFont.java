package dev.loki.lovisual.render.engine.text.font.faces;

import dev.loki.lovisual.render.engine.texture.Texture;
import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;
import dev.loki.lovisual.render.engine.text.font.Font;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;

public record EmptyGlyphFont(int height) implements GlyphFont {
    public EmptyGlyphFont(int height) {
        this.height = Math.max(1, height);
    }

    @Override
    public Texture getTexture() {
        return null;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public double getWidth(String string, int length) {
        return 0;
    }

    @Override
    public double render(MeshBuilder mesh, String string, double x, double y, RenderColor color, double scale) {
        return x;
    }

    @Override
    public double renderGradient(MeshBuilder mesh, String string, double x, double y, double scale, Font.GlyphGradient gradient) {
        return x;
    }

    @Override
    public double emitGlyphs(String string, double x, double y, double scale, GlyphConsumer consumer) {
        return x;
    }

    @Override
    public void close() {
    }
}
