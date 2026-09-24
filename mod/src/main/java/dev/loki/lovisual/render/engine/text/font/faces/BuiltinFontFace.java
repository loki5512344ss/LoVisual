package dev.loki.lovisual.render.engine.text.font.faces;

import java.io.InputStream;
import dev.loki.lovisual.render.engine.text.font.FontFace;
import dev.loki.lovisual.render.engine.text.font.FontUtils;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;

public class BuiltinFontFace extends FontFace {
    private final String name;
    private final boolean atlasOnly;

    public BuiltinFontFace(FontInfo info, String name) {
        this(info, name, false);
    }

    public BuiltinFontFace(FontInfo info, String name, boolean atlasOnly) {
        super(info);
        this.name = name;
        this.atlasOnly = atlasOnly;
    }

    @Override
    public InputStream toStream() {
        if (atlasOnly) {
            throw new RuntimeException("Builtin font " + name + " is atlas-only.");
        }
        InputStream in = FontUtils.streamBuiltin(name);
        if (in == null) throw new RuntimeException("Failed to load builtin font " + name + ".");
        return in;
    }

    @Override
    public boolean isAtlasOnly() {
        return atlasOnly;
    }

    @Override
    public String toString() {
        return super.toString() + " (builtin)";
    }
}
