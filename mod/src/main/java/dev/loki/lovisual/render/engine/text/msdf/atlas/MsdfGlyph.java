package dev.loki.lovisual.render.engine.text.msdf.atlas;

/**
 * Глиф MSDF-атласа: метрики в экранных пикселях + UV-координаты.
 */
public record MsdfGlyph(float advance, float left, float bottom, float right, float top,
                        float u0, float v0, float u1, float v1, boolean visible) {

    /**
     * Границы прямоугольника из msdf-атласа (формат msdf-bmfont).
     */
    public record Bounds(float left, float bottom, float right, float top) {
    }

    public static MsdfGlyph from(float advance,
                                 Bounds plane,
                                 Bounds atlas,
                                 boolean yOriginBottom,
                                 int atlasWidth,
                                 int atlasHeight,
                                 float invW,
                                 float invH) {
        if (plane == null || atlas == null) {
            return new MsdfGlyph(advance, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, false);
        }

        float left = plane.left();
        float right = plane.right();
        float top = plane.top();
        float bottom = plane.bottom();

        // The renderer expects plane Y values in screen-space (down-positive).
        // MSDF atlases exported with bottom-origin store plane Y in up-positive space.
        if (yOriginBottom) {
            top = -top;
            bottom = -bottom;
        }

        float aLeft = atlas.left();
        float aRight = atlas.right();
        float aTop = atlas.top();
        float aBottom = atlas.bottom();

        if (yOriginBottom) {
            aTop = atlasHeight - aTop;
            aBottom = atlasHeight - aBottom;
        }

        float u0 = aLeft * invW;
        float u1 = aRight * invW;
        float v0 = aTop * invH;
        float v1 = aBottom * invH;

        return new MsdfGlyph(advance, left, bottom, right, top, u0, v0, u1, v1, true);
    }
}
