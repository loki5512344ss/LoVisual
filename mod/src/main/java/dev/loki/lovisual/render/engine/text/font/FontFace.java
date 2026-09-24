package dev.loki.lovisual.render.engine.text.font;

import java.io.InputStream;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;

public abstract class FontFace {
    public final FontInfo info;

    protected FontFace(FontInfo info) {
        this.info = info;
    }

    public abstract InputStream toStream();

    public boolean isAtlasOnly() {
        return false;
    }

    @Override
    public String toString() {
        return info.toString();
    }
}
