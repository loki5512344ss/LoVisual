package dev.loki.lovisual.render.engine.text.msdf.atlas;

import dev.loki.lovisual.render.engine.texture.Texture;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

/**
 * Загруженный MSDF-атлас: текстура, метрики и таблица глифов.
 * Счётчик ссылок: несколько размеров MsdfFont делят один атлас.
 */
public final class MsdfAtlas {
    public final Texture texture;
    public final float distanceRange;
    public final int width;
    public final int height;
    public final float lineHeight;
    public final float ascender;
    public final Int2ObjectOpenHashMap<MsdfGlyph> glyphs;
    public final @Nullable MsdfGlyph fallback;
    private int refs;

    MsdfAtlas(Texture texture,
              float distanceRange,
              int width,
              int height,
              float lineHeight,
              float ascender,
              Int2ObjectOpenHashMap<MsdfGlyph> glyphs,
              @Nullable MsdfGlyph fallback) {
        this.texture = texture;
        this.distanceRange = distanceRange;
        this.width = width;
        this.height = height;
        this.lineHeight = lineHeight;
        this.ascender = ascender;
        this.glyphs = glyphs;
        this.fallback = fallback;
    }

    /**
     * Загружает атлас для builtin-шрифта; null и причина — через {@link #lastError()}.
     */
    public static @Nullable MsdfAtlas load(dev.loki.lovisual.render.engine.text.font.info.FontInfo info) {
        return MsdfAtlasLoader.load(info);
    }

    public static String lastError() {
        return MsdfAtlasLoader.lastError();
    }

    public void retain() {
        refs++;
    }

    public void release() {
        refs--;
        if (refs <= 0 && texture != null) {
            texture.close();
        }
    }
}
