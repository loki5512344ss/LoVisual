/*
 * This file is part of the LoVisual Client distribution.
 * Copyright (c) 2026 loki5512344.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package dev.loki.lovisual.config.profile.snapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import dev.loki.lovisual.config.profile.codec.ConfigProfileEntry;
import dev.loki.lovisual.config.profile.snapshot.ConfigProfileMeta;

public record ConfigProfileSnapshot(
        ConfigProfileMeta meta,
        List<ConfigProfileEntry> entries
) {
    public ConfigProfileSnapshot {
        if (meta == null) throw new IllegalArgumentException("profile meta is null");
        List<ConfigProfileEntry> copy = new ArrayList<>();
        if (entries != null) copy.addAll(entries);
        entries = Collections.unmodifiableList(copy);
    }
}
