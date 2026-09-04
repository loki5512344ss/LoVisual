/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.features.security;

import net.minecraft.network.chat.contents.TranslatableContents;
import org.jspecify.annotations.Nullable;

public final class FilteredTranslatableTextContent extends TranslatableContents {

    @Nullable
    private final String resolved;

    public FilteredTranslatableTextContent(String key, @Nullable String fallback, Object[] args, @Nullable String resolved) {
        super(key, fallback, args);
        this.resolved = resolved;
    }

    @Nullable
    public String getResolved() {
        return resolved;
    }
}
