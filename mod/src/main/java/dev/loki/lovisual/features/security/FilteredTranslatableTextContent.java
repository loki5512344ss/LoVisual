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
