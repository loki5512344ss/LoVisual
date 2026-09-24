package dev.loki.lovisual.render.engine.text.font.faces;

import java.util.ArrayList;
import java.util.List;
import dev.loki.lovisual.render.engine.text.font.FontFace;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;

public class FontFamily {
    private final String name;
    private final List<FontFace> fonts = new ArrayList<>();

    public FontFamily(String name) {
        this.name = name;
    }

    public boolean addFont(FontFace font) {
        return fonts.add(font);
    }

    public boolean hasType(FontInfo.Type type) {
        return get(type) != null;
    }

    public FontFace get(FontInfo.Type type) {
        if (type == null) return null;
        for (FontFace font : fonts) {
            if (font.info.type().equals(type)) {
                return font;
            }
        }
        return null;
    }

    public String getName() {
        return name;
    }
}
