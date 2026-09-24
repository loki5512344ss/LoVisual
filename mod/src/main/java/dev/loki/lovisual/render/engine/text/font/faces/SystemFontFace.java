package dev.loki.lovisual.render.engine.text.font.faces;

import java.io.InputStream;
import java.nio.file.Path;
import dev.loki.lovisual.render.engine.text.font.FontFace;
import dev.loki.lovisual.render.engine.text.font.FontUtils;
import dev.loki.lovisual.render.engine.text.font.info.FontInfo;

public class SystemFontFace extends FontFace {
    private final Path path;

    public SystemFontFace(FontInfo info, Path path) {
        super(info);
        this.path = path;
    }

    @Override
    public InputStream toStream() {
        if (!path.toFile().exists()) {
            throw new RuntimeException("Tried to load font that no longer exists.");
        }

        InputStream in = FontUtils.streamFile(path.toFile());
        if (in == null) throw new RuntimeException("Failed to load font from " + path + ".");
        return in;
    }

    @Override
    public String toString() {
        return super.toString() + " (" + path + ")";
    }
}
