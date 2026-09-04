/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.profile;

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
