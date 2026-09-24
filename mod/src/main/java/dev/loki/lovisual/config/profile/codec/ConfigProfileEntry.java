package dev.loki.lovisual.config.profile.codec;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ConfigProfileEntry(
        String ownerId,
        String displayName,
        Map<String, Object> values
) {
    public ConfigProfileEntry {
        if (ownerId == null) ownerId = "";
        if (displayName == null || displayName.isBlank()) displayName = ownerId;
        Map<String, Object> copy = new LinkedHashMap<>();
        if (values != null) copy.putAll(values);
        values = Collections.unmodifiableMap(copy);
    }
}
