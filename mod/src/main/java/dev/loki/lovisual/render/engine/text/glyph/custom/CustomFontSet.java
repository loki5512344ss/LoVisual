/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.glyph.custom;

import dev.loki.lovisual.render.engine.text.font.Font;
import dev.loki.lovisual.render.engine.text.font.FontFace;
import dev.loki.lovisual.render.engine.text.font.FontUtils;
import dev.loki.lovisual.render.engine.text.font.faces.BuiltinFontFace;
import dev.loki.lovisual.render.engine.text.font.faces.EmptyGlyphFont;
import dev.loki.lovisual.render.engine.text.font.info.FontDebugStats;
import dev.loki.lovisual.render.engine.text.glyph.CustomTextRenderer.GlyphSelection;
import dev.loki.lovisual.render.engine.text.glyph.GlyphFont;
import dev.loki.lovisual.render.engine.text.msdf.MsdfFont;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

/**
 * Набор размеров GlyphFont одного FontFace: MSDF для builtin-шрифтов, иначе
 * растровый Font из TTF-байтов. Владеет выбором размера и пересозданием.
 */
public final class CustomFontSet {
    private final FontFace fontFace;
    private GlyphFont[] fonts;
    private int fontIndex;

    public CustomFontSet(FontFace fontFace) {
        this.fontFace = fontFace;
        this.fonts = createFonts();
    }

    public GlyphFont current() {
        return fontIndex >= 0 && fontIndex < fonts.length ? fonts[fontIndex] : null;
    }

    public GlyphFont base() {
        return fonts[0];
    }

    public GlyphSelection select(double requestedScale, boolean big) {
        if (big) {
            fontIndex = fonts.length - 1;
        } else {
            double scaleA = Math.floor(requestedScale * 10) / 10;
            int scaleI;
            if (scaleA >= 3) scaleI = 5;
            else if (scaleA >= 2.5) scaleI = 4;
            else if (scaleA >= 2) scaleI = 3;
            else if (scaleA >= 1.5) scaleI = 2;
            else scaleI = 1;
            fontIndex = scaleI - 1;
        }

        double fontScale = fonts[fontIndex].height() / 27.0;
        double scale = 1 + (requestedScale - fontScale) / fontScale;
        return new GlyphSelection(fonts[fontIndex], fontScale, scale);
    }

    /**
     * Выбор с одним retry: если выбранный шрифт не готов (атлас не догрузился) —
     * пересоздаёт набор и выбирает заново.
     */
    public GlyphSelection prepare(double requestedScale, boolean big) {
        GlyphSelection selection = select(requestedScale, big);
        if (!selection.font().isReady() && rebuild()) {
            selection = select(requestedScale, big);
        }
        return selection;
    }

    public boolean rebuild() {
        GlyphFont[] old = this.fonts;
        GlyphFont[] fresh;
        try {
            fresh = createFonts();
        } catch (Exception e) {
            return false;
        }

        this.fonts = fresh;
        this.fontIndex = Math.max(0, Math.min(fontIndex, fresh.length - 1));

        if (old != null) {
            for (GlyphFont f : old) {
                f.close();
            }
        }
        return true;
    }

    public void close() {
        if (fonts == null) return;
        for (GlyphFont f : fonts) {
            f.close();
        }
    }

    private GlyphFont[] createFonts() {
        boolean msdfEligible = fontFace instanceof BuiltinFontFace
                && !FontUtils.isIconFamily(fontFace.info.family());
        if (msdfEligible) {
            FontDebugStats.noteMsdfAttempt(fontFace.info);
            GlyphFont[] msdf = MsdfFont.tryCreate(fontFace);
            if (msdf != null) {
                FontDebugStats.noteMsdfSuccess(fontFace.info);
                return msdf;
            }
            FontDebugStats.noteMsdfFallback(fontFace.info, MsdfFont.getLastError());
            if (fontFace.isAtlasOnly()) {
                return createEmptyFonts();
            }
        }

        byte[] bytes = FontUtils.readBytes(fontFace.toStream());
        ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
        try {
            buffer.put(bytes).flip();

            GlyphFont[] result = new GlyphFont[5];
            for (int i = 0; i < result.length; i++) {
                result[i] = new Font(buffer, (int) Math.round(27 * ((i * 0.5) + 1)));
            }
            return result;
        } finally {
            MemoryUtil.memFree(buffer);
        }
    }

    private static GlyphFont[] createEmptyFonts() {
        GlyphFont[] result = new GlyphFont[5];
        for (int i = 0; i < result.length; i++) {
            int height = (int) Math.round(27 * ((i * 0.5) + 1));
            result[i] = new EmptyGlyphFont(height);
        }
        return result;
    }
}
