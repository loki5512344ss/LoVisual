/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.render.engine.text.backend;

import dev.loki.lovisual.render.engine.Texture;

public record GlyphAtlasPage(String id, Texture texture, int width, int height, boolean msdf) {
}
