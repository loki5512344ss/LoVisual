/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.msdf;

import dev.loki.lovisual.render.engine.color.RenderColor;
import dev.loki.lovisual.render.engine.text.font.Font;
import dev.loki.lovisual.render.engine.text.font.FontFace;
import dev.loki.lovisual.render.engine.text.font.FontUtils;
import dev.loki.lovisual.render.engine.text.font.faces.BuiltinFontFace;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;
import dev.loki.lovisual.render.engine.text.msdf.atlas.MsdfAtlas;
import dev.loki.lovisual.render.engine.text.msdf.atlas.MsdfGlyph;
import dev.loki.lovisual.render.engine.texture.Texture;
import dev.loki.lovisual.render.engine.uniform.MeshBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MSDF-шрифтовый фасад GlyphFont: размеры от одного атласа, LRU-кэш ширин.
 * Загрузка — {@link MsdfAtlas}, генерация квадов — {@link MsdfGlyphDrawer}.
 */
public final class MsdfFont implements GlyphFont {
    private static final int FONT_SIZES = 5;
    private static final float BASE_HEIGHT = 27f;
    private static final int WIDTH_CACHE_LIMIT = 512;
    private final MsdfAtlas atlas;
    private final float pixelScale;
    private final int height;
    private final LinkedHashMap<WidthKey, Double> widthCache = new LinkedHashMap<>(WIDTH_CACHE_LIMIT, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<WidthKey, Double> eldest) {
            return size() > WIDTH_CACHE_LIMIT;
        }
    };

    private MsdfFont(MsdfAtlas atlas, float targetHeight) {
        this.atlas = atlas;
        this.height = Math.max(1, Math.round(targetHeight));
        this.pixelScale = targetHeight / atlas.lineHeight;
        atlas.retain();
    }

    public static GlyphFont[] tryCreate(FontFace face) {
        if (!(face instanceof BuiltinFontFace)) return null;
        if (FontUtils.isIconFamily(face.info.family())) return null;

        MsdfAtlas atlas = MsdfAtlas.load(face.info);
        if (atlas == null) return null;

        GlyphFont[] fonts = new GlyphFont[FONT_SIZES];
        for (int i = 0; i < FONT_SIZES; i++) {
            float target = BASE_HEIGHT * ((i * 0.5f) + 1f);
            fonts[i] = new MsdfFont(atlas, target);
        }
        return fonts;
    }

    public static String getLastError() {
        return MsdfAtlas.lastError();
    }

    @Override
    public Texture getTexture() {
        return atlas.texture;
    }

    @Override
    public boolean isReady() {
        return atlas.texture != null && atlas.texture.isReady();
    }

    @Override
    public double getWidth(String string, int length) {
        if (string == null || string.isEmpty() || length <= 0) return 0.0;
        int safeLength = Math.min(length, string.length());
        WidthKey key = new WidthKey(string, safeLength);
        Double cached = widthCache.get(key);
        if (cached != null) return cached;

        double width = 0;
        for (int i = 0; i < safeLength; ) {
            int cp = string.codePointAt(i);
            MsdfGlyph g = atlas.glyphs.get(cp);
            if (g == null) g = atlas.fallback;
            if (g != null) width += g.advance() * pixelScale;
            i += Character.charCount(cp);
        }
        widthCache.put(key, width);
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public double render(MeshBuilder mesh, String string, double x, double y, RenderColor color, double scale) {
        return MsdfGlyphDrawer.render(atlas, pixelScale, mesh, string, x, y, color, scale);
    }

    @Override
    public double emitGlyphs(String string, double x, double y, double scale, GlyphConsumer consumer) {
        return MsdfGlyphDrawer.emitGlyphs(atlas, pixelScale, string, x, y, scale, consumer);
    }

    @Override
    public boolean hasGlyph(int codePoint) {
        return atlas.glyphs.containsKey(codePoint);
    }

    @Override
    public double getAdvance(int codePoint) {
        MsdfGlyph g = atlas.glyphs.get(codePoint);
        return g != null ? g.advance() * pixelScale : 0.0;
    }

    @Override
    public double renderGradient(MeshBuilder mesh, String string, double x, double y, double scale, Font.GlyphGradient gradient) {
        return MsdfGlyphDrawer.renderGradient(atlas, pixelScale, mesh, string, x, y, scale, gradient);
    }

    @Override
    public double renderQuadGradient(MeshBuilder mesh, String string, double x, double y, double scale, Font.GlyphQuadGradient gradient) {
        return MsdfGlyphDrawer.renderQuadGradient(atlas, pixelScale, mesh, string, x, y, scale, gradient);
    }

    @Override
    public void close() {
        widthCache.clear();
        atlas.release();
    }

    @Override
    public boolean isMsdf() {
        return true;
    }

    @Override
    public float getPxRange() {
        return atlas.distanceRange;
    }

    @Override
    public int getAtlasWidth() {
        return atlas.width;
    }

    @Override
    public int getAtlasHeight() {
        return atlas.height;
    }

    private record WidthKey(String text, int length) {
    }
}
